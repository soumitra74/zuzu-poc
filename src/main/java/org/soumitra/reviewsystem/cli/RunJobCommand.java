package org.soumitra.reviewsystem.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "run-job",
    description = "Run a job to process reviews from S3 bucket"
)
public class RunJobCommand implements Callable<Integer> {
    
    @Autowired
    private CLIService cliService;
    
    @Option(names = {"-b", "--bucket"}, description = "S3 bucket name", required = true)
    private String bucket;
    
    @Option(names = {"-p", "--page-size"}, description = "Page size for processing (default: 10)")
    private int pageSize = 10;
    
    @Option(names = {"-t", "--trigger-type"}, description = "Trigger type (default: MANUAL)")
    private String triggerType = "MANUAL";
    
    @Option(names = {"-n", "--notes"}, description = "Job notes")
    private String notes = "CLI triggered job";

    @Override
    public Integer call() throws Exception {
        cliService.runJob(bucket, pageSize, triggerType, notes);
        return 0;
    }
} 