package org.soumitra.reviewsystem.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "process-records",
    description = "Process review records from the database"
)
public class ProcessRecordsCommand implements Callable<Integer> {
    
    @Autowired
    private CLIService cliService;
    
    @Option(names = {"-b", "--batch-size"}, description = "Batch size for processing (default: 10)")
    private int batchSize = 10;
    
    @Option(names = {"-t", "--trigger-type"}, description = "Trigger type (default: MANUAL)")
    private String triggerType = "MANUAL";
    
    @Option(names = {"-n", "--notes"}, description = "Job notes")
    private String notes = "CLI triggered record processing job";

    @Override
    public Integer call() throws Exception {
        cliService.processRecords(batchSize, triggerType, notes);
        return 0;
    }
} 