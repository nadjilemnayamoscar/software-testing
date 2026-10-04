import java.util.*;
import java.util.regex.Pattern;

/**
 * Software Testing project - Employee Management System of organization X.
 * Functions: employee management (FR1-FR4), payroll and payment processing
 * (FR5-FR6) and reporting (FR7).
 *
 * Compile: javac EmployeeManagementSystem.java EmployeeManagementSystemTest.java
 * Run app: java EmployeeManagementSystem
 * Run tests: java EmployeeManagementSystemTest
 */
public class EmployeeManagementSystem {

    static class Employee {
        private final String id;
        private String name;
        private String department;
        private double baseSalary;

        public Employee(String id, String name, String department, double baseSalary) {
            this.id = id; this.name = name; this.department = department; this.baseSalary = baseSalary;
        }
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public double getBaseSalary() { return baseSalary; }
        public void setName(String name) { this.name = name; }
        public void setDepartment(String department) { this.department = department; }
        public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
    }

    static class Payment {
        public final String employeeId, month;
        public final double gross, tax, net;
        public Payment(String employeeId, String month, double gross, double tax, double net) {
            this.employeeId = employeeId; this.month = month;
            this.gross = gross; this.tax = tax; this.net = net;
        }
    }

    /** FR1-FR4: employee management (add, update, delete, search). */
    static class EmployeeManager {
        private final Map<String, Employee> employees = new LinkedHashMap<>();

        private static boolean blank(String s) { return s == null || s.isBlank(); }

        public Employee addEmployee(String id, String name, String dept, double salary) {
            if (blank(id))   throw new IllegalArgumentException("Employee ID is required");
            if (blank(name)) throw new IllegalArgumentException("Employee name is required");
            if (blank(dept)) throw new IllegalArgumentException("Department is required");
            if (salary <= 0) throw new IllegalArgumentException("Salary must be greater than zero");
            if (employees.containsKey(id)) throw new IllegalArgumentException("Duplicate employee ID");
            Employee e = new Employee(id, name, dept, salary);
            employees.put(id, e);
            return e;
        }

        public Employee updateEmployee(String id, String name, String dept, double salary) {
            Employee e = employees.get(id);
            if (e == null) throw new IllegalArgumentException("Employee not found");
            if (blank(name) || blank(dept) || salary <= 0)
                throw new IllegalArgumentException("Invalid update data");
            e.setName(name); e.setDepartment(dept); e.setBaseSalary(salary);
            return e;
        }

        public void deleteEmployee(String id) {
            if (employees.remove(id) == null) throw new IllegalArgumentException("Employee not found");
        }

        public Employee findById(String id) { return employees.get(id); }

        public Collection<Employee> listAll() { return employees.values(); }
    }

    /** FR5-FR6: payroll calculation and payment processing. */
    static class PayrollProcessor {
        private static final Pattern MONTH = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");
        private final EmployeeManager manager;
        private final List<Payment> payments = new ArrayList<>();

        public PayrollProcessor(EmployeeManager manager) { this.manager = manager; }

        /** Monthly tax: 0% up to 100,000; 10% on 100,000-300,000; 20% above 300,000. */
        public double calculateTax(double gross) {
            if (gross <= 100000) {
                return 0;
            } else if (gross <= 300000) {
                return (gross - 100000) * 0.10;
            } else {
                return 20000 + (gross - 300000) * 0.20;
            }
        }

        /** Overtime paid at 1.5 x hourly rate (base / 160). Valid hours: 0..100. */
        public double calculateOvertimePay(double baseSalary, double hours) {
            if (hours < 0 || hours > 100) throw new IllegalArgumentException("Overtime hours must be between 0 and 100");
            return (baseSalary / 160.0) * 1.5 * hours;
        }

        public Payment processPayment(String id, String month, double overtimeHours) {
            if (month == null || !MONTH.matcher(month).matches())
                throw new IllegalArgumentException("Month must be in format YYYY-MM");
            Employee e = manager.findById(id);
            if (e == null) throw new IllegalArgumentException("Employee not found");
            for (Payment p : payments) {
                if (p.employeeId.equals(id) && p.month.equals(month))
                    throw new IllegalStateException("Payment already processed for this month");
            }
            double gross = e.getBaseSalary() + calculateOvertimePay(e.getBaseSalary(), overtimeHours);
            double tax = calculateTax(gross);
            Payment p = new Payment(id, month, gross, tax, gross - tax);
            payments.add(p);
            return p;
        }

        public List<Payment> getPayments() { return payments; }
    }

    /** FR7: reporting. */
    static class ReportGenerator {
        private final EmployeeManager manager;
        private final PayrollProcessor payroll;

        public ReportGenerator(EmployeeManager m, PayrollProcessor p) { this.manager = m; this.payroll = p; }

        public String payrollReport(String month) {
            int count = 0;
            double totalTax = 0, totalNet = 0;
            for (Payment p : payroll.getPayments()) {
                if (p.month.equals(month)) {
                    count++;
                    totalTax += p.tax;
                    totalNet += p.net;
                }
            }
            if (count == 0) return "No payments for " + month;
            return String.format(Locale.ROOT, "Payroll %s: employees=%d, totalTax=%.2f, totalNet=%.2f",
                    month, count, totalTax, totalNet);
        }

        public Map<String, double[]> departmentSummary() {
            Map<String, double[]> summary = new TreeMap<>();
            for (Employee e : manager.listAll()) {
                double[] s = summary.computeIfAbsent(e.getDepartment(), k -> new double[2]);
                s[0] += 1;
                s[1] += e.getBaseSalary();
            }
            return summary;
        }
    }

    // ---------------------------------------------------------------
    // Console menu
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        EmployeeManager manager = new EmployeeManager();
        PayrollProcessor payroll = new PayrollProcessor(manager);
        ReportGenerator reports = new ReportGenerator(manager, payroll);

        while (true) {
            System.out.println("\n=== Software Testing - Employee Management System ===");
            System.out.println("1. Add employee");
            System.out.println("2. Update employee");
            System.out.println("3. Delete employee");
            System.out.println("4. Search employee");
            System.out.println("5. Process payment");
            System.out.println("6. Payroll report");
            System.out.println("7. Department summary");
            System.out.println("0. Exit");
            System.out.print("Choice: ");
            if (!in.hasNextLine()) break;
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1": {
                        System.out.print("ID: ");         String id = in.nextLine();
                        System.out.print("Name: ");       String name = in.nextLine();
                        System.out.print("Department: "); String dept = in.nextLine();
                        System.out.print("Salary: ");     double sal = Double.parseDouble(in.nextLine());
                        manager.addEmployee(id, name, dept, sal);
                        System.out.println("Employee added.");
                        break;
                    }
                    case "2": {
                        System.out.print("ID: ");         String id = in.nextLine();
                        System.out.print("New name: ");   String name = in.nextLine();
                        System.out.print("New department: "); String dept = in.nextLine();
                        System.out.print("New salary: "); double sal = Double.parseDouble(in.nextLine());
                        manager.updateEmployee(id, name, dept, sal);
                        System.out.println("Employee updated.");
                        break;
                    }
                    case "3": {
                        System.out.print("ID: "); manager.deleteEmployee(in.nextLine());
                        System.out.println("Employee deleted.");
                        break;
                    }
                    case "4": {
                        System.out.print("ID: "); Employee e = manager.findById(in.nextLine());
                        System.out.println(e == null ? "Employee not found."
                            : e.getId() + " | " + e.getName() + " | " + e.getDepartment() + " | " + e.getBaseSalary());
                        break;
                    }
                    case "5": {
                        System.out.print("ID: ");             String id = in.nextLine();
                        System.out.print("Month (YYYY-MM): "); String month = in.nextLine();
                        System.out.print("Overtime hours: "); double ot = Double.parseDouble(in.nextLine());
                        Payment p = payroll.processPayment(id, month, ot);
                        System.out.printf(Locale.ROOT, "Gross=%.2f Tax=%.2f Net=%.2f%n", p.gross, p.tax, p.net);
                        break;
                    }
                    case "6": {
                        System.out.print("Month (YYYY-MM): ");
                        System.out.println(reports.payrollReport(in.nextLine()));
                        break;
                    }
                    case "7": {
                        for (Map.Entry<String, double[]> en : reports.departmentSummary().entrySet())
                            System.out.printf(Locale.ROOT, "%s: count=%d total=%.2f%n",
                                en.getKey(), (int) en.getValue()[0], en.getValue()[1]);
                        break;
                    }
                    case "0":
                        System.out.println("Goodbye.");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (NumberFormatException ex) {
                System.out.println("Error: a number was expected.");
            } catch (RuntimeException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
    }
}
