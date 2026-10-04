import java.util.*;
import java.util.function.Supplier;

public class EmployeeManagementSystemTest {
    static int pass = 0, fail = 0;
    static List<String> out = new ArrayList<>();

    static String f(double d) { return String.format(Locale.ROOT, "%.2f", d); }

    static String attempt(Supplier<String> s) {
        try { return s.get(); }
        catch (Exception e) { return "REJECTED: " + e.getMessage(); }
    }

    static void check(String id, String expected, Supplier<String> body) {
        String actual = attempt(body);
        boolean ok = expected.equals(actual);
        if (ok) pass++; else fail++;
        String line = id + "\t" + expected + "\t" + actual + "\t" + (ok ? "PASS" : "FAIL");
        out.add(line);
        System.out.println(line);
    }

    static EmployeeManagementSystem.EmployeeManager mgrWith(String... ids) {
        EmployeeManagementSystem.EmployeeManager m = new EmployeeManagementSystem.EmployeeManager();
        for (String id : ids) m.addEmployee(id, "Name " + id, "IT", 160000);
        return m;
    }

    public static void main(String[] args) {
        // ================= BLACK-BOX (specification based) =================
        check("BB-01", "ADDED: E001 Alice IT 150000.00", () -> {
            EmployeeManagementSystem.Employee e = new EmployeeManagementSystem.EmployeeManager().addEmployee("E001", "Alice", "IT", 150000);
            return "ADDED: " + e.getId() + " " + e.getName() + " " + e.getDepartment() + " " + f(e.getBaseSalary());
        });
        check("BB-02", "REJECTED: Duplicate employee ID", () -> {
            EmployeeManagementSystem.EmployeeManager m = mgrWith("E001");
            m.addEmployee("E001", "Bob", "HR", 120000); return "ADDED";
        });
        check("BB-03", "REJECTED: Salary must be greater than zero", () -> {
            new EmployeeManagementSystem.EmployeeManager().addEmployee("E002", "Carol", "HR", 0); return "ADDED";
        });
        check("BB-04", "REJECTED: Employee not found", () -> {
            mgrWith("E001").updateEmployee("E999", "Dan", "IT", 100000); return "UPDATED";
        });
        check("BB-05", "DELETED; find(E001)=null", () -> {
            EmployeeManagementSystem.EmployeeManager m = mgrWith("E001"); m.deleteEmployee("E001");
            return "DELETED; find(E001)=" + m.findById("E001");
        });
        EmployeeManagementSystem.PayrollProcessor pp = new EmployeeManagementSystem.PayrollProcessor(new EmployeeManagementSystem.EmployeeManager());
        check("BB-06", "0.00", () -> f(pp.calculateTax(100000)));
        check("BB-07", "0.10", () -> f(pp.calculateTax(100001)));
        check("BB-08", "20000.20", () -> f(pp.calculateTax(300001)));
        check("BB-09", "REJECTED: Overtime hours must be between 0 and 100", () -> {
            pp.calculateOvertimePay(160000, 101); return "OK";
        });
        check("BB-10", "gross=175000.00 tax=7500.00 net=167500.00", () -> {
            EmployeeManagementSystem.EmployeeManager m = mgrWith("E001");
            EmployeeManagementSystem.Payment p = new EmployeeManagementSystem.PayrollProcessor(m).processPayment("E001", "2026-09", 10);
            return "gross=" + f(p.gross) + " tax=" + f(p.tax) + " net=" + f(p.net);
        });
        check("BB-11", "PAID: net=154000.00 tax=6000.00 (no overtime)", () -> {
            EmployeeManagementSystem.EmployeeManager m = mgrWith("E001");
            EmployeeManagementSystem.Payment p = new EmployeeManagementSystem.PayrollProcessor(m).processPayment("E001", "2026-09", 0);
            return "PAID: net=" + f(p.net) + " tax=" + f(p.tax) + " (no overtime)";
        });
        check("BB-12", "REJECTED: Payment already processed for this month", () -> {
            EmployeeManagementSystem.EmployeeManager m = mgrWith("E001"); EmployeeManagementSystem.PayrollProcessor p = new EmployeeManagementSystem.PayrollProcessor(m);
            p.processPayment("E001", "2026-09", 0); p.processPayment("E001", "2026-09", 0); return "PAID";
        });
        check("BB-13", "REJECTED: Month must be in format YYYY-MM", () -> {
            new EmployeeManagementSystem.PayrollProcessor(mgrWith("E001")).processPayment("E001", "2026-13", 0); return "PAID";
        });
        check("BB-14", "Payroll 2026-09: employees=2, totalTax=12000.00, totalNet=308000.00", () -> {
            EmployeeManagementSystem.EmployeeManager m = mgrWith("E001", "E002"); EmployeeManagementSystem.PayrollProcessor p = new EmployeeManagementSystem.PayrollProcessor(m);
            p.processPayment("E001", "2026-09", 0); p.processPayment("E002", "2026-09", 0);
            return new EmployeeManagementSystem.ReportGenerator(m, p).payrollReport("2026-09");
        });
        check("BB-15", "HR: count=2 total=250000.00 | IT: count=1 total=160000.00", () -> {
            EmployeeManagementSystem.EmployeeManager m = new EmployeeManagementSystem.EmployeeManager();
            m.addEmployee("E001", "A", "IT", 160000);
            m.addEmployee("E002", "B", "HR", 100000);
            m.addEmployee("E003", "C", "HR", 150000);
            Map<String, double[]> s = new EmployeeManagementSystem.ReportGenerator(m, new EmployeeManagementSystem.PayrollProcessor(m)).departmentSummary();
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, double[]> en : s.entrySet()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(en.getKey()).append(": count=").append((int) en.getValue()[0])
                  .append(" total=").append(f(en.getValue()[1]));
            }
            return sb.toString();
        });

        // ================= WHITE-BOX (structure based) =================
        // addEmployee: decisions D1..D5 (blank id, blank name, blank dept, salary<=0, duplicate)
        check("WB-01", "REJECTED: Employee ID is required", () -> { new EmployeeManagementSystem.EmployeeManager().addEmployee(" ", "A", "IT", 1); return "ADDED"; });
        check("WB-02", "REJECTED: Employee name is required", () -> { new EmployeeManagementSystem.EmployeeManager().addEmployee("E1", null, "IT", 1); return "ADDED"; });
        check("WB-03", "REJECTED: Department is required", () -> { new EmployeeManagementSystem.EmployeeManager().addEmployee("E1", "A", "", 1); return "ADDED"; });
        check("WB-04", "REJECTED: Salary must be greater than zero", () -> { new EmployeeManagementSystem.EmployeeManager().addEmployee("E1", "A", "IT", -5); return "ADDED"; });
        check("WB-05", "REJECTED: Duplicate employee ID", () -> { EmployeeManagementSystem.EmployeeManager m = mgrWith("E1"); m.addEmployee("E1", "A", "IT", 5); return "ADDED"; });
        // calculateTax: three branches
        check("WB-06", "0.00", () -> f(pp.calculateTax(50000)));
        check("WB-07", "10000.00", () -> f(pp.calculateTax(200000)));
        check("WB-08", "30000.00", () -> f(pp.calculateTax(350000)));
        // processPayment: decisions D1 (null month || bad regex), D2 (employee null), D3 (duplicate), success
        check("WB-09", "REJECTED: Month must be in format YYYY-MM", () -> { new EmployeeManagementSystem.PayrollProcessor(mgrWith("E1")).processPayment("E1", null, 0); return "PAID"; });
        check("WB-10", "REJECTED: Month must be in format YYYY-MM", () -> { new EmployeeManagementSystem.PayrollProcessor(mgrWith("E1")).processPayment("E1", "Sept-2026", 0); return "PAID"; });
        check("WB-11", "REJECTED: Employee not found", () -> { new EmployeeManagementSystem.PayrollProcessor(mgrWith("E1")).processPayment("E9", "2026-09", 0); return "PAID"; });
        check("WB-12", "REJECTED: Payment already processed for this month", () -> {
            EmployeeManagementSystem.PayrollProcessor p = new EmployeeManagementSystem.PayrollProcessor(mgrWith("E1"));
            p.processPayment("E1", "2026-09", 0); p.processPayment("E1", "2026-09", 5); return "PAID"; });
        check("WB-13", "PAID: net=154000.00; payments stored=1", () -> {
            EmployeeManagementSystem.PayrollProcessor p = new EmployeeManagementSystem.PayrollProcessor(mgrWith("E1"));
            EmployeeManagementSystem.Payment pay = p.processPayment("E1", "2026-09", 0);
            return "PAID: net=" + f(pay.net) + "; payments stored=" + p.getPayments().size(); });
        // payrollReport loop: 0, 1, many iterations (month filter true/false)
        check("WB-14", "[No payments for 2026-09] [Payroll 2026-09: employees=1, totalTax=6000.00, totalNet=154000.00] [Payroll 2026-09: employees=2, totalTax=12000.00, totalNet=308000.00]", () -> {
            EmployeeManagementSystem.EmployeeManager m = mgrWith("E1", "E2"); EmployeeManagementSystem.PayrollProcessor p = new EmployeeManagementSystem.PayrollProcessor(m);
            EmployeeManagementSystem.ReportGenerator r = new EmployeeManagementSystem.ReportGenerator(m, p);
            String a = r.payrollReport("2026-09");
            p.processPayment("E1", "2026-09", 0);
            p.processPayment("E1", "2026-08", 0); // other month: filter-false branch
            String b = r.payrollReport("2026-09");
            p.processPayment("E2", "2026-09", 0);
            String c = r.payrollReport("2026-09");
            return "[" + a + "] [" + b + "] [" + c + "]"; });

        // calculateOvertimePay: hours < 0 branch
        check("WB-15", "REJECTED: Overtime hours must be between 0 and 100", () -> { pp.calculateOvertimePay(160000, -1); return "OK"; });

        System.out.println("TOTAL=" + (pass + fail) + " PASS=" + pass + " FAIL=" + fail);
    }
}
