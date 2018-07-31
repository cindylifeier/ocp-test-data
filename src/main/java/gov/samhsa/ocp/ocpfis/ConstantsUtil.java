package gov.samhsa.ocp.ocpfis;

public class ConstantsUtil {

    public static final String SSN_DISPLAY = "SSN";
    public static final String SSN_URI = "http://hl7.org/fhir/sid/us-ssn";
    public static final String MEDICARE_NUMBER_DISPLAY = "Medicare Number";
    public static final String MEDICARE_NUMBER_URI = "http://hl7.org/fhir/sid/us-medicare";
    public static final String IND_TAX_ID_DISPLAY = "Individual Tax ID";
    public static final String IND_TAX_ID_URI = "urn:oid:2.16.840.1.113883.4.2";
    public static final String ORG_TAX_ID_DISPLAY = "Organization Tax ID";
    public static final String ORG_TAX_ID_URI = "urn:oid:2.16.840.1.113883.4.4";
    public static final String NPI_DISPLAY = "NPI";
    public static final String NPI_URI = "http://hl7.org/fhir/sid/us-npi";


    public static final String PHONE_SYSTEM = "phone";
    public static final String EMAIL_SYSTEM = "email";

    public static final String ACTIVE_STATUS = "active";

    public static final String HCS_DEFAULT_CATEGORY_DISPLAY = "Adoption";
    public static final String HCS_CATEGORY_SYSTEM = "http://hl7.org/fhir/service-category";
    public static final String HCS_DEFAULT_TYPE_DISPLAY = "Aged Care Assessment";
    public static final String HCS_TYPE_SYSTEM = "http://hl7.org/fhir/service-type";
    public static final String HCS_DEFAULT_SPECIALITY_DISPLAY = "Adult mental illness";
    public static final String HCS_SPECIALITY_SYSTEM = ""; //Add
    public static final String HCS_DEFAULT_REFERRAL_DISPLAY = "phone";
    public static final String HCS_REFERRAL_SYSTEM = "http://hl7.org/fhir/service-referral-method";

    public static final String PRACTITIONER_DEFAULT_SPECIALITY_CODE = "103GC0700X";
    public static final String PRACTITIONER_DEFAULT_SPECIALITY_DISPLAY = "Clinical";
    public static final String PRACTITIONER_SPECIALITY_SYSTEM = ""; //Add
    public static final String PRACTITIONER_DEFAULT_ROLE_DISPLAY = "Counselor";
    public static final String PRACTITIONER_ROLE_SYSTEM = ""; //Add

    public static final int EPISODE_OF_CARE_END_PERIOD = 1;



}
