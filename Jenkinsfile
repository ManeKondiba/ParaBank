def publishResults(String resultGroup) {
    // Keep the original target/reports -> ../../screenshots relationship.
    withEnv(["RESULT_GROUP=${resultGroup}"]) {
        powershell '''
            $ErrorActionPreference = 'Stop'
            $resultRoot = Join-Path $env:WORKSPACE ('.jenkins-results/' + $env:RESULT_GROUP)
            foreach ($relativePath in @('target/surefire-reports', 'target/reports', 'target/logs', 'target/api-evidence', 'target/browser-diagnostics', 'screenshots')) {
                $sourcePath = Join-Path $env:WORKSPACE $relativePath
                if (Test-Path -LiteralPath $sourcePath -PathType Container) {
                    $destinationPath = Join-Path $resultRoot $relativePath
                    New-Item -ItemType Directory -Path $destinationPath -Force | Out-Null
                    Get-ChildItem -LiteralPath $sourcePath -Force | ForEach-Object {
                        Copy-Item -LiteralPath $_.FullName -Destination $destinationPath -Recurse -Force
                    }
                }
            }
        '''
    }
    archiveArtifacts artifacts: ".jenkins-results/${resultGroup}/**/*", allowEmptyArchive: true
    // A compilation failure may produce no XML; Maven's exit code still fails the build.
    junit testResults: ".jenkins-results/${resultGroup}/target/surefire-reports/TEST-*.xml",
          allowEmptyResults: currentBuild.currentResult != 'SUCCESS'
}

pipeline {
    agent { label 'windows' }

    tools { jdk 'jdk17' }

    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
        timeout(time: 90, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
    }

    triggers { pollSCM('H/5 * * * *') }

    parameters {
        choice(name: 'UI_SUITE', choices: ['none', 'smoke', 'sanity', 'regression', 'cross-browser'],
               description: 'Manual builds only. none runs compilation and framework unit tests.')
        choice(name: 'API_SUITE', choices: ['none', 'smoke', 'regression'],
               description: 'Manual builds only. Starts a controlled local ParaBank instance for API tests.')
        choice(name: 'BROWSER', choices: ['chrome', 'edge', 'firefox'],
               description: 'Browser for smoke/sanity/regression; cross-browser always runs all three.')
        string(name: 'APP_URL', defaultValue: 'https://parabank.parasoft.com/parabank/index.htm', trim: true,
               description: 'ParaBank test URL. Use a controlled deployment for reliable UI results.')
    }

    stages {
        stage('Checkout') {
            steps {
                // This must be a dedicated Jenkins workspace, never the developer checkout.
                deleteDir()
                checkout scm
            }
        }

        stage('Build and unit tests') {
            steps {
                script { env.UNIT_STARTED = 'true' }
                bat '''@echo off
call mvnw.cmd -B -ntp -Punit clean verify
exit /b %ERRORLEVEL%
'''
            }
            post {
                always {
                    script {
                        if (env.UNIT_STARTED == 'true') {
                            publishResults('unit')
                        }
                    }
                }
            }
        }

        stage('UI tests') {
            when {
                allOf {
                    expression { params.UI_SUITE != 'none' }
                    triggeredBy 'UserIdCause'
                    not { triggeredBy 'SCMTrigger' }
                }
            }
            steps {
                script {
                    def profiles = ['smoke': 'smoke', 'sanity': 'sanity', 'regression': 'regression', 'cross-browser': 'cross-browser']
                    def profile = profiles[params.UI_SUITE]
                    if (!profile || !(params.BROWSER in ['chrome', 'edge', 'firefox'])) {
                        error('Choose a supported UI_SUITE and BROWSER.')
                    }
                    // Empty withEnv values unset inherited browser overrides for the XML suite.
                    def browser = params.UI_SUITE == 'cross-browser' ? '' : params.BROWSER
                    withEnv(["PARABANK_APP_URL=${params.APP_URL}", "PARABANK_BROWSER=${browser}",
                             'PARABANK_HEADLESS=true']) {
                        powershell '''
                            $ErrorActionPreference = 'Stop'
                            $applicationUri = $null
                            if (![Uri]::TryCreate($env:PARABANK_APP_URL, [UriKind]::Absolute, [ref]$applicationUri) -or
                                $applicationUri.Scheme -notin @('http', 'https') -or
                                [string]::IsNullOrWhiteSpace($applicationUri.Host) -or
                                ![string]::IsNullOrEmpty($applicationUri.UserInfo)) {
                                throw 'APP_URL must be an absolute HTTP(S) URL without embedded credentials.'
                            }
                        '''
                        // Clear only generated paths inside this Jenkins workspace so even a
                        // launcher failure cannot publish the unit output as UI results.
                        dir('target') { deleteDir() }
                        dir('screenshots') { deleteDir() }
                        env.UI_STARTED = 'true'
                        // profile is selected from the fixed map; APP_URL never enters shell text.
                        bat """@echo off
call mvnw.cmd -B -ntp -P${profile} -DexcludedGroups=Unit clean verify
exit /b %ERRORLEVEL%
"""
                    }
                }
            }
            post {
                always {
                    script {
                        if (env.UI_STARTED == 'true') {
                            publishResults('ui')
                        }
                    }
                }
            }
        }
        stage('API tests') {
            when {
                allOf {
                    expression { params.API_SUITE != 'none' }
                    triggeredBy 'UserIdCause'
                    not { triggeredBy 'SCMTrigger' }
                }
            }
            steps {
                script {
                    def profile = ['smoke': 'api-smoke', 'regression': 'api-regression'][params.API_SUITE]
                    if (!profile) { error('Choose a supported API_SUITE.') }
                    // Earlier stages have archived results. Start the app after clearing output;
                    // never run Maven clean while its local server lives below target/.
                    dir('target/surefire-reports') { deleteDir() }
                    dir('target/reports') { deleteDir() }
                    dir('target/api-evidence') { deleteDir() }
                    env.API_STARTED = 'true'
                    powershell '.\\scripts\\start-parabank.ps1 -Port 8081 -DatabasePort 9001 -ShutdownPort 8006'
                    withEnv(['PARABANK_API_BASE_URL=http://127.0.0.1:8081/parabank/services/bank']) {
                        bat """@echo off
call mvnw.cmd -B -ntp -P${profile} verify
exit /b %ERRORLEVEL%
"""
                    }
                }
            }
            post {
                always {
                    script {
                        if (env.API_STARTED == 'true') {
                            try {
                                powershell '.\\scripts\\stop-parabank.ps1'
                            } finally {
                                try {
                                    powershell '.\\scripts\\capture-api-logs.ps1'
                                } finally {
                                    publishResults('api')
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
