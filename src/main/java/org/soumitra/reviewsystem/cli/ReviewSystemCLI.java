package org.soumitra.reviewsystem.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IFactory;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "review-system",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Review System CLI for processing hotel reviews from S3",
    subcommands = {
        RunJobCommand.class,
        ListJobsCommand.class,
        ListFilesCommand.class
    }
)
public class ReviewSystemCLI implements Callable<Integer> {

    @Autowired
    private IFactory factory;

    @Override
    public Integer call() throws Exception {
        return new CommandLine(this, factory).execute();
    }
} 