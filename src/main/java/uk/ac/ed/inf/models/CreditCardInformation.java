package uk.ac.ed.inf.models;

public class CreditCardInformation {
    private String creditCardNumber;
    private String creditCardExpiry;
    private String cvv;

    // Getters and setters
    public String getCreditCardNumber() { return creditCardNumber; }
    public void setCreditCardNumber(String creditCardNumber) { this.creditCardNumber = creditCardNumber; }

    public String getCreditCardExpiry() { return creditCardExpiry; }
    public void setCreditCardExpiry(String creditCardExpiry) { this.creditCardExpiry = creditCardExpiry; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
}