package org.soumitra.reviewsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.soumitra.reviewsystem.cli.ReviewSystemCLI;
import picocli.CommandLine;
import picocli.spring.PicocliSpringFactory;

@SpringBootApplication
public class ReviewSystemApplication implements CommandLineRunner {

    @Autowired
    private ReviewSystemCLI reviewSystemCLI;

    @Autowired
    private ApplicationContext applicationContext;

    public static void main(String[] args) {
        SpringApplication.run(ReviewSystemApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            // CLI mode - run the CLI commands
            PicocliSpringFactory factory = new PicocliSpringFactory(applicationContext);
            int exitCode = new CommandLine(reviewSystemCLI, factory).execute(args);
            System.exit(exitCode);
        } else {
            // Web application mode - keep running
            System.out.println("Review System Application started in web mode.");
            System.out.println("Use command line arguments to run CLI commands.");
            System.out.println("Example: java -jar app.jar run-job --bucket hotel-reviews");
        }
    }
} 