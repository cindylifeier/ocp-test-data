package gov.samhsa.ocp.ocpfis;

public class OCPAdminUAAHelper {

    public static void createOCPAdmin() {

        UAAHelper.createEntityInUAA("ocpAdmin ocpAdminLastName", null, "ocp.role.ocpAdmin", null, 3);

    }
}
