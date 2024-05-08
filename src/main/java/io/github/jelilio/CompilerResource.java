package io.github.jelilio;

import io.github.jelilio.config.CompilerConfig;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/compiler")
public class CompilerResource {

    @Inject
    CompilerConfig compilerConfig;

    @GET
    @Path("")
    public Uni<String> index() {
        return Uni.createFrom().item("Compiler API");
    }

    @GET
    @Path("/languages")
    public Uni<Set<String>> getLanguages() {
        return Uni.createFrom().item(compilerConfig.languages().keySet());
    }

    @GET
    @Path("/languages-versions")
    public Uni<Map<String, List<String>>> getLangVersions() {
        return Uni.createFrom().item(compilerConfig.languages());
    }
}
