package utilities;

public final class AppRoute {

    private AppRoute() {
    }

    public static final String INDEX = "index.htm";
    public static final String REGISTER = "register.htm";
    public static final String LOOKUP = "lookup.htm";
    public static final String OVERVIEW = "overview.htm";
    public static final String OPEN_ACCOUNT = "openaccount.htm";
    public static final String TRANSFER = "transfer.htm";
    public static final String BILL_PAY = "billpay.htm";
    public static final String FIND_TRANSACTIONS = "findtrans.htm";
    public static final String UPDATE_PROFILE = "updateprofile.htm";
    public static final String REQUEST_LOAN = "requestloan.htm";
    public static final String ACTIVITY = "activity.htm";
    public static final String TRANSACTION = "transaction.htm";

    public static String activity(String accountId) {
        return ACTIVITY + "?id=" + accountId;
    }

    public static String transaction(String transactionId) {
        return TRANSACTION + "?id=" + transactionId;
    }
}
