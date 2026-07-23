package com.enonic.kubernetes.operator.api.info;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller("/apis/operator.enonic.cloud/v1")
public class InfoApi {
    @Value("${operator.api.version}")
    String apiVersion;

    @Value("${operator.api.group}")
    String group;

    @SuppressWarnings("DuplicatedCode")
    @Get("/")
    @Produces("application/json")
    public Map<String, Object> api() {
        Map<String, Object> admissionResource = new HashMap<>();
        admissionResource.put("group", "admission.k8s.io");
        admissionResource.put("kind", "AdmissionReview");
        admissionResource.put("name", "validations");
        admissionResource.put("namespaced", false);
        admissionResource.put("singularName", "");
        admissionResource.put("verbs", Collections.singletonList("create"));
        admissionResource.put("version", "v1");

        Map<String, Object> mutationResource = new HashMap<>();
        mutationResource.put("group", "admission.k8s.io");
        mutationResource.put("kind", "AdmissionReview");
        mutationResource.put("name", "mutations");
        mutationResource.put("namespaced", false);
        mutationResource.put("singularName", "");
        mutationResource.put("verbs", Collections.singletonList("create"));
        mutationResource.put("version", "v1");

        Map<String, Object> res = new HashMap<>();
        res.put("apiVersion", "v1");
        res.put("groupVersion", group + "/" + apiVersion);
        res.put("kind", "APIResourceList");
        res.put("resources", Arrays.asList(admissionResource, mutationResource));
        return res;
    }
}
