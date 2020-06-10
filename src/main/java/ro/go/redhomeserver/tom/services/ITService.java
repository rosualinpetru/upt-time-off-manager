package ro.go.redhomeserver.tom.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ro.go.redhomeserver.tom.dtos.CredentialsEmail;
import ro.go.redhomeserver.tom.models.Account;
import ro.go.redhomeserver.tom.models.Employee;
import ro.go.redhomeserver.tom.models.IssueRequest;
import ro.go.redhomeserver.tom.repositories.AccountRepository;
import ro.go.redhomeserver.tom.repositories.EmployeeRepository;
import ro.go.redhomeserver.tom.repositories.IssueRequestRepository;

import javax.transaction.SystemException;
import java.util.*;

@Service
public class ITService {

    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;
    private final IssueRequestRepository issueRequestRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ITService(AccountRepository accountRepository, EmailService emailService, EmployeeRepository employeeRepository, IssueRequestRepository issueRequestRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
        this.issueRequestRepository = issueRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void generateAccount(int employeeId, int teamLeaderId) throws SystemException {
        Optional<Employee> employeeOptional = employeeRepository.findById(employeeId);
        Optional<Account> teamLeaderOptional = accountRepository.findByEmployee_Id(teamLeaderId);

        if (!employeeOptional.isPresent() || !teamLeaderOptional.isPresent())
            throw new SystemException();

        String username;
        String password;
        String salt;

        Random random = new Random();
        int length = 16;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(random.nextInt(characters.length()));
        }
        password = new String(text);

        salt = password.substring(length - 6, length);
        String passwordToSend = password.substring(0, length - 6);
        String hashedPassword;

        hashedPassword = passwordEncoder.encode(password);

        int index = (employeeOptional.get().getName()).indexOf(" ");
        String name = employeeOptional.get().getName();
        username = (name.substring(index + 1, index + 2) + name.substring(0, index)).toLowerCase();
        if (accountRepository.findByUsername(username).isPresent()) {
            int i = 1;
            String aux = username;
            do {
                username = aux + i;
                i++;
            } while (accountRepository.findByUsername(username).isPresent());

        }

        Account acc = new Account(username, hashedPassword, salt, employeeOptional.get(), teamLeaderOptional.get());
        CredentialsEmail data = new CredentialsEmail(acc, "Account data", username, passwordToSend);

        try {
            emailService.sendEmail(data);
            accountRepository.save(acc);
        } catch (MailException | NullPointerException e) {
            throw new SystemException();
        }
    }

    public void informItAboutSystemError(int employeeId) {
        issueRequestRepository.save(new IssueRequest("The user with id: " + employeeId + "doesn't have an account due to some system problems!", null));
    }
}