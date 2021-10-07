package com.enonic.kubernetes.client;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        EnonicKubernetesClient client = new EnonicKubernetesClient();
        System.out.println(client.namespaces().list().getItems());
        System.out.println(client.operator().version());
        System.out.println(client.xp7().snapshots().
                inNamespace("my-namespace").
                withName("my-deployment").
                withAnyNode().
                list());
//        MgmtSnapshots s = client.xp()
//                .management()
//                .snapshots()
//                .inNamespace("asd")
//                .withName("asd")
//                .withNodeGroup("asd");
    }
}
