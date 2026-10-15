package com.smartsenergy.microgrid.utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Advanced Sri Lankan National Identity Card (NIC) Validator & Decoder.
 * 
 * Verifies authenticity based on Department for Registration of Persons (DRP) specifications:
 * 1. Old NIC format (Pre-2016):
 *    - 9 digits followed by 'V' (voter) or 'X' (non-voter) -> total 10 chars.
 *    - Digits 1-2: Birth Year (19XX)
 *    - Digits 3-5: Day count of year (001-366 for Male, 501-866 for Female)
 *    - Digits 6-8: Daily registration serial number (cannot be 000)
 *    - Digit 9: Check digit
 * 2. New NIC format (2016 onwards):
 *    - 12 digits total.
 *    - Digits 1-4: 4-digit Birth Year
 *    - Digits 5-7: Day count of year (001-366 for Male, 501-866 for Female)
 *    - Digits 8-11: Serial number (cannot be 0000)
 *    - Digit 12: Check digit
 */
public final class NicValidator {

    private static final Pattern OLD_NIC_REGEX = Pattern.compile("^[0-9]{9}[vVxX]$");
    private static final Pattern NEW_NIC_REGEX = Pattern.compile("^[0-9]{12}$");

    // Days in each month for NIC day count (February is allocated 29 days in standard DRP table)
    private static final int[] DAYS_IN_MONTHS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final String[] MONTH_NAMES = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    // Obvious dummy test patterns to reject
    private static final Set<String> DUMMY_PATTERNS = new HashSet<>(Arrays.asList(
            "000000000V", "111111111V", "123456789V", "987654321V",
            "000000000X", "111111111X", "123456789X",
            "000000000000", "111111111111", "123456789012"
    ));

    private NicValidator() {}

    /**
     * Cleans and normalizes NIC: trims whitespace and capitalizes 'v'/'x' to 'V'/'X'.
     */
    public static String format(String nic) {
        if (nic == null) return "";
        return nic.trim().toUpperCase();
    }

    /**
     * Checks if the given NIC is authentic and structurally valid.
     */
    public static boolean isValid(String nic) {
        return validate(nic) == null;
    }

    /**
     * Validates the NIC and returns a descriptive error message if invalid, or null if valid.
     */
    public static String validate(String rawNic) {
        if (rawNic == null || rawNic.trim().isEmpty()) {
            return "NIC number is required";
        }

        String nic = format(rawNic);

        // Reject obvious dummy sequences
        if (DUMMY_PATTERNS.contains(nic)) {
            return "Invalid NIC: dummy or test sequence is not allowed";
        }

        int len = nic.length();
        if (len != 10 && len != 12) {
            if (len == 9 && nic.matches("^[0-9]+$")) {
                return "Old 9-digit NIC must end with 'V' or 'X' (e.g. " + nic + "V)";
            }
            return "Invalid NIC length (" + len + " chars). Must be 10 characters (Old) or 12 digits (New)";
        }

        boolean isOldFormat = (len == 10);

        if (isOldFormat) {
            if (!OLD_NIC_REGEX.matcher(nic).matches()) {
                return "Invalid Old NIC format. Must be 9 digits followed by 'V' or 'X' (e.g. 951234567V)";
            }
        } else {
            if (!NEW_NIC_REGEX.matcher(nic).matches()) {
                return "Invalid New NIC format. Must contain exactly 12 digits (e.g. 199512304567)";
            }
        }

        int birthYear;
        int dayValue;
        String serialNumber;

        if (isOldFormat) {
            birthYear = 1900 + Integer.parseInt(nic.substring(0, 2));
            dayValue = Integer.parseInt(nic.substring(2, 5));
            serialNumber = nic.substring(5, 8);
        } else {
            birthYear = Integer.parseInt(nic.substring(0, 4));
            dayValue = Integer.parseInt(nic.substring(4, 7));
            serialNumber = nic.substring(7, 11);
        }

        // Serial numbers cannot be all zeros
        if (isAllZeros(serialNumber)) {
            return "Invalid NIC serial number (" + serialNumber + ")";
        }

        // Year & Age validation
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int age = currentYear - birthYear;

        if (birthYear < 1900 || birthYear > currentYear) {
            return "Invalid birth year in NIC: " + birthYear;
        }

        // User must be at least 16 years old to hold an NIC/account
        if (age < 16) {
            return "Invalid NIC: holder must be at least 16 years old (NIC indicates birth year " + birthYear + ")";
        }
        if (age > 115) {
            return "Invalid NIC: birth year " + birthYear + " exceeds realistic human lifespan";
        }

        // Day of Year validation
        // Male: 001 - 366
        // Female: 501 - 866 (500 + 1..366)
        boolean isMale = (dayValue >= 1 && dayValue <= 366);
        boolean isFemale = (dayValue >= 501 && dayValue <= 866);

        if (!isMale && !isFemale) {
            return "Invalid day of year (" + dayValue + "). Sri Lankan NIC days must be 001-366 (Male) or 501-866 (Female)";
        }

        return null; // Valid!
    }

    private static boolean isAllZeros(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != '0') return false;
        }
        return true;
    }

    /**
     * Decoded information from a valid Sri Lankan NIC.
     */
    public static class NicDetails {
        public final boolean isValid;
        public final String formattedNic;
        public final String formatType; // "Old (9 digits + V/X)" or "New (12 digits)"
        public final int birthYear;
        public final String dateOfBirth; // e.g. "1995-May-03"
        public final int age;
        public final String gender; // "Male" or "Female"
        public final int dayOfYear;
        public final String voterStatus; // "Eligible Voter (V)" or "Non-voter (X)" or "Standard (New)"
        public final String errorMessage;

        private NicDetails(boolean isValid, String formattedNic, String formatType,
                           int birthYear, String dateOfBirth, int age, String gender,
                           int dayOfYear, String voterStatus, String errorMessage) {
            this.isValid = isValid;
            this.formattedNic = formattedNic;
            this.formatType = formatType;
            this.birthYear = birthYear;
            this.dateOfBirth = dateOfBirth;
            this.age = age;
            this.gender = gender;
            this.dayOfYear = dayOfYear;
            this.voterStatus = voterStatus;
            this.errorMessage = errorMessage;
        }

        public static NicDetails valid(String formattedNic, String formatType, int birthYear,
                                      String dateOfBirth, int age, String gender, int dayOfYear, String voterStatus) {
            return new NicDetails(true, formattedNic, formatType, birthYear, dateOfBirth, age, gender, dayOfYear, voterStatus, null);
        }

        public static NicDetails invalid(String errorMessage) {
            return new NicDetails(false, "", "", 0, "", 0, "", 0, "", errorMessage);
        }
    }

    /**
     * Extracts full details (Date of Birth, Age, Gender, Format) from an authentic NIC.
     */
    public static NicDetails getDetails(String rawNic) {
        String error = validate(rawNic);
        if (error != null) {
            return NicDetails.invalid(error);
        }

        String nic = format(rawNic);
        boolean isOldFormat = (nic.length() == 10);

        int birthYear;
        int rawDays;
        String formatType;
        String voterStatus;

        if (isOldFormat) {
            char lastChar = nic.charAt(9);
            formatType = "Old Format";
            voterStatus = (lastChar == 'V') ? "Eligible Voter (V)" : "Non-voter (X)";
            birthYear = 1900 + Integer.parseInt(nic.substring(0, 2));
            rawDays = Integer.parseInt(nic.substring(2, 5));
        } else {
            formatType = "New Format";
            voterStatus = "National Identity Card (12-digit)";
            birthYear = Integer.parseInt(nic.substring(0, 4));
            rawDays = Integer.parseInt(nic.substring(4, 7));
        }

        String gender;
        int dayOfYear;
        if (rawDays > 500) {
            gender = "Female";
            dayOfYear = rawDays - 500;
        } else {
            gender = "Male";
            dayOfYear = rawDays;
        }

        // Calculate Month and Day of Month
        int tempDays = dayOfYear;
        int monthIndex = 0;
        for (int i = 0; i < DAYS_IN_MONTHS.length; i++) {
            if (tempDays <= DAYS_IN_MONTHS[i]) {
                monthIndex = i;
                break;
            }
            tempDays -= DAYS_IN_MONTHS[i];
        }
        int dayOfMonth = Math.max(1, tempDays);
        String dateOfBirth = String.format("%04d-%s-%02d", birthYear, MONTH_NAMES[monthIndex], dayOfMonth);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int age = Math.max(0, currentYear - birthYear);

        return NicDetails.valid(nic, formatType, birthYear, dateOfBirth, age, gender, dayOfYear, voterStatus);
    }
}
