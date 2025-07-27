package org.soumitra.reviewsystem.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "list-jobs",
    description = "List recent job runs"
)
public class ListJobsCommand implements Callable<Integer> {
    
    @Autowired
    private CLIService cliService;
    
    @Option(names = {"-l", "--limit"}, description = "Number of jobs to list (default: 10)")
    private int limit = 10;
    
    @Option(names = {"-s", "--status"}, description = "Filter by status")
    private String status;

    @Override
    public Integer call() throws Exception {
        cliService.listJobs(limit, status);
        return 0;
    }
} 