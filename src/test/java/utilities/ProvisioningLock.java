package utilities;

/** Serializes UI customer/account creation within one JVM to avoid ParaBank sequence races. */
public final class ProvisioningLock {
    public static final Object MONITOR = new Object();

    private ProvisioningLock() {
    }
}
