package com.enonic.kubernetes.operator.api.info;

import com.enonic.kubernetes.client.v1.api.operator.OperatorVersion;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;
import java.util.Properties;

@Controller("/apis/operator.enonic.cloud/v1")
public class OperatorApi {
    @Get("/operator/version")
    @Produces("application/json")
    public OperatorVersion info()
            throws IOException, ParseException {
        Properties git = new Properties();
        git.load(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("git.properties")));

        OperatorVersion operatorVersion = new OperatorVersion();
        operatorVersion.setVersion(git.getProperty("git.build.version"));
        operatorVersion.setBuildDate(git.getProperty("git.build.time"));
        operatorVersion.setGitCommit(git.getProperty("git.commit.id"));
        operatorVersion.setGitTags(git.getProperty("git.tags"));
        operatorVersion.setGitTreeState(git.getProperty("git.dirty").equals("true") ? "dirty" : "clean");
        return operatorVersion;
    }
}
