package org.soumitra.reviewsystem.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "list-files",
    description = "List files in S3 bucket"
)
public class ListFilesCommand implements Callable<Integer> {
    
    @Autowired
    private CLIService cliService;
    
    @Parameters(description = "S3 bucket name", arity = "1")
    private String bucket;
    
    @Option(names = {"-p", "--prefix"}, description = "File prefix filter")
    private String prefix;

    @Override
    public Integer call() throws Exception {
        cliService.listFiles(bucket, prefix);
        return 0;
    }
} 