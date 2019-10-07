package be.zvz.kotlininside.http;

public class HttpException extends RuntimeException {
    private final int statusCode;
    private final String errorMessage;

    public HttpException(int statusCode, String errorMessage) {
        super("Error Code : " + statusCode + ", Error Message" + errorMessage);
        this.errorMessage = errorMessage;
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}