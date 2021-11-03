package com.enonic.kubernetes.operator.api.info;

import com.enonic.kubernetes.client.v1.api.operator.OperatorVersion;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;
import java.util.Properties;

@ApplicationScoped
@Path("/apis/operator.enonic.cloud/v1")
public class OperatorApi {
    @GET
    @Path("/operator/version")
    @Produces("application/json")
    public OperatorVersion info()
            throws IOException, ParseException {
        Properties git = new Properties();
        git.load(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("git.properties")));

        return new OperatorVersion()
                .withVersion(git.getProperty("git.build.version"))
                .withBuildDate(git.getProperty("git.build.time"))
                .withGitCommit(git.getProperty("git.commit.id"))
                .withGitTags(git.getProperty("git.tags"))
                .withGitTreeState(git.getProperty("git.dirty").equals("true") ? "dirty" : "clean");
    }
}
