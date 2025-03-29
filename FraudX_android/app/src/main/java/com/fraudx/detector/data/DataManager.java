package com.fraudx.detector.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataManager {
    private static DataManager instance;
    
    // Sample headlines - replace these with real data from your API
    private List<String> topHeadlines = Arrays.asList(
        "New Banking Regulations Introduced to Combat Financial Fraud",
        "Cybersecurity Experts Warn of Sophisticated Phishing Attacks Targeting Mobile Users",
        "Major Bank Data Breach Exposes Customer Information",
        "New Artificial Intelligence Tool Detects 95% of Online Scams",
        "Government Launches Initiative to Educate Public About Digital Security",
        "Digital Wallet Security Concerns Rise as Mobile Payments Increase",
        "Financial Advisors Warn Against Investment Schemes Promising Quick Returns",
        "Social Media Companies Join Forces to Combat Fake Accounts and Fraud",
        "Study Shows Millennials Most Vulnerable to Identity Theft",
        "Ransomware Attacks Cost Businesses $20 Billion Last Year",
        "New Blockchain Technology Promises More Secure Online Transactions",
        "AI-Powered Fraud Detection Systems Show Promise in Early Tests",
        "Consumer Protection Agency Issues Warning About Crypto Investment Scams",
        "Financial Literacy Program Aims to Reduce Vulnerability to Scams",
        "Insurance Companies Now Offering Cybersecurity Protection Policies"
    );
    
    // Sample scams - replace these with real data from your API
    private List<String> topScams = Arrays.asList(
        "Fake Job Offers Requesting Payment for Training Materials",
        "Cryptocurrency Investment Schemes Promising Unrealistic Returns",
        "Tech Support Scammers Claiming Your Computer Has a Virus",
        "Romance Scams on Dating Apps Asking for Financial Help",
        "Fake Shopping Websites with No Intention to Deliver Products",
        "Phishing Emails Impersonating Banks Requesting Account Information",
        "Fraudulent COVID-19 Relief Programs Requesting Personal Information",
        "Lottery Scams Claiming You've Won a Prize That Requires a Fee to Collect",
        "Fake Charity Appeals Following Natural Disasters",
        "Advance Fee Scams Requesting Money for a Future Larger Payment",
        "Real Estate Scams Collecting Deposits for Non-Existent Properties",
        "QR Code Scams Leading to Malicious Websites",
        "Fake Investment Apps That Steal Your Deposit",
        "Utility Company Impersonators Threatening to Cut Off Service",
        "Gift Card Scams Requesting Payment via Gift Cards"
    );
    
    private DataManager() {
        // Private constructor for singleton
    }
    
    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }
    
    public List<String> getTopHeadlines() {
        // In a real app, you'd fetch this from a network API or database
        return new ArrayList<>(topHeadlines);
    }
    
    public List<String> getTopScams() {
        // In a real app, you'd fetch this from a network API or database
        return new ArrayList<>(topScams);
    }
    
    // Methods to update data when refreshed from API
    public void updateTopHeadlines(List<String> headlines) {
        if (headlines != null && !headlines.isEmpty()) {
            this.topHeadlines = new ArrayList<>(headlines);
        }
    }
    
    public void updateTopScams(List<String> scams) {
        if (scams != null && !scams.isEmpty()) {
            this.topScams = new ArrayList<>(scams);
        }
    }
} 