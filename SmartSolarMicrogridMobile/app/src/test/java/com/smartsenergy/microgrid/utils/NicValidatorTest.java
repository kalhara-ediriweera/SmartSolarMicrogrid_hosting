package com.smartsenergy.microgrid.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class NicValidatorTest {

    @Test
    public void testRandomDigitsRejected() {
        // User example: "123457893" (9 digits with no V/X)
        assertFalse(NicValidator.isValid("123457893"));
        String err = NicValidator.validate("123457893");
        assertNotNull(err);
        assertTrue(err.contains("must end with 'V' or 'X'") || err.contains("Invalid NIC length"));
    }

    @Test
    public void testDummySequencesRejected() {
        assertFalse(NicValidator.isValid("123456789V"));
        assertFalse(NicValidator.isValid("000000000V"));
        assertFalse(NicValidator.isValid("111111111V"));
        assertFalse(NicValidator.isValid("123456789012"));
    }

    @Test
    public void testValidOldNicMale() {
        String nic = "951234567V";
        assertTrue(NicValidator.isValid(nic));

        NicValidator.NicDetails details = NicValidator.getDetails(nic);
        assertTrue(details.isValid);
        assertEquals("951234567V", details.formattedNic);
        assertEquals("Old Format", details.formatType);
        assertEquals(1995, details.birthYear);
        assertEquals("Male", details.gender);
        assertEquals("1995-May-03", details.dateOfBirth);
        assertEquals("Eligible Voter (V)", details.voterStatus);
    }

    @Test
    public void testValidOldNicFemale() {
        String nic = "956234567V"; // 623 - 500 = 123 (May 03)
        assertTrue(NicValidator.isValid(nic));

        NicValidator.NicDetails details = NicValidator.getDetails(nic);
        assertTrue(details.isValid);
        assertEquals(1995, details.birthYear);
        assertEquals("Female", details.gender);
        assertEquals("1995-May-03", details.dateOfBirth);
    }

    @Test
    public void testValidNewNicMale() {
        String nic = "199512304567";
        assertTrue(NicValidator.isValid(nic));

        NicValidator.NicDetails details = NicValidator.getDetails(nic);
        assertTrue(details.isValid);
        assertEquals("New Format", details.formatType);
        assertEquals(1995, details.birthYear);
        assertEquals("Male", details.gender);
        assertEquals("1995-May-03", details.dateOfBirth);
    }

    @Test
    public void testValidNewNicFemale() {
        String nic = "200062304567";
        assertTrue(NicValidator.isValid(nic));

        NicValidator.NicDetails details = NicValidator.getDetails(nic);
        assertTrue(details.isValid);
        assertEquals(2000, details.birthYear);
        assertEquals("Female", details.gender);
        assertEquals("2000-May-03", details.dateOfBirth);
    }

    @Test
    public void testInvalidDays() {
        // Day 400 does not exist
        assertFalse(NicValidator.isValid("954001234V"));
        // Day 900 does not exist
        assertFalse(NicValidator.isValid("959001234V"));
        // Day 000 does not exist
        assertFalse(NicValidator.isValid("950001234V"));
    }

    @Test
    public void testZeroSerialNumberRejected() {
        // Serial number 000 is invalid
        assertFalse(NicValidator.isValid("951230004V"));
    }

    @Test
    public void testAgeValidation() {
        // Baby born in 2024 cannot have old NIC or prosumer account (< 16 years old)
        assertFalse(NicValidator.isValid("202412301234"));
    }
}
