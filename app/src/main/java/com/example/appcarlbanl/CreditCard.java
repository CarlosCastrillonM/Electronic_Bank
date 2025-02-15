package com.example.appcarlbanl;

public class CreditCard {
    String numberCard;
    String fullname;
    int balance;

    public CreditCard(String numberCard, String fullname, int balance) {
        this.numberCard = numberCard;
        this.fullname = fullname;
        this.balance = balance;
    }

    public String getNumberCard() {
        return numberCard;
    }

    public void setNumberCard(String numberCard) {
        this.numberCard = numberCard;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "CreaditCard{" +
                "numberCard='" + numberCard + '\'' +
                ", fullname='" + fullname + '\'' +
                ", balance=" + balance +
                '}';
    }
}
