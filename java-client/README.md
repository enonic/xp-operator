<h1>Enonic XP Kubernetes Operator Client</h1>

This is the Java client for the [Enonic XP Kubernetes Operator](https://github.com/enonic/xp-operator). This client includes the latest CRD model and bindings for the [fabric8io/kubernetes-client](https://github.com/fabric8io/kubernetes-client).

- [Installation](#installation)
  - [Maven](#maven)
  - [Gradle](#gradle)
- [K8s CRD deserialization](#k8s-crd-deserialization)
- [K8s clients creation](#k8s-clients-creation)
- [CRUD](#crud)
  - [Standard resources](#standard-resources)
  - [Custom resources](#custom-resources)
- [Watching with websockets (short lived watching)](#watching-with-websockets-short-lived-watching)
  - [Standard resources](#standard-resources-1)
  - [Custom resources](#custom-resources-1)
- [Watching with informers (long lived watching)](#watching-with-informers-long-lived-watching)
  - [Standard resources](#standard-resources-2)
  - [Custom resources](#custom-resources-2)

## Installation

### Maven

```xml
<dependency>
  <groupId>com.enonic.kubernetes</groupId>
  <artifactId>client</artifactId>
  <version>${k8s-client-version}</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    include "com.enonic.kubernetes:client:${k8s-client-version}"
}
```

## K8s CRD deserialization

If you are using a custom jackson object mapper you can add the deserializer to it like so:

```java
ObjectMapper objectMapper = new ObjectMapper();
CrdMappingProvider.registerDeserializer(objectMapper);
```

## K8s clients creation

```java
// Create config
Config config = new ConfigBuilder().
        withMasterUrl( "https://<CLUSTER>" ).
        withNewClientCertData( "<CA_CERT>" ).
        withOauthToken( "<TOKEN>" ).
        build();

// Create client
EnonicKubernetesClient client = new DefaultEnonicKubernetesClient( new DefaultKubernetesClient( config ).inAnyNamespace() );
```

## CRUD

### Standard resources

> **_NOTE:_** See more details at the [fabric8io/kubernetes-client](https://github.com/fabric8io/kubernetes-client) repository.

```java
// Create
ConfigMap configMap = client.k8s().configMaps().
    inNamespace( "my-namespace" ).
    withName( "my-config-map" ).
    create( new ConfigMapBuilder().
        withData( Map.of("my", "data") ).
        build() );

// Create or Replace
configMap = client.k8s().configMaps().
    inNamespace( "my-namespace" ).
    withName( "my-config-map" ).
    createOrReplace( new ConfigMapBuilder().
        withData( Map.of("my", "new-data-with-create-or-replace") ).
        build() );

// Patch
configMap = client.k8s().configMaps().
    inNamespace( "my-namespace" ).
    withName( "my-config-map" ).
    patch( new ConfigMapBuilder().
        withData( Map.of("my", "new-data-with-patch") ).
        build() );

// Replace
configMap = client.k8s().configMaps().
    inNamespace( "my-namespace" ).
    withName( "my-config-map" ).
    replace( new ConfigMapBuilder().
        withData( Map.of("my", "new-data-with-replace") ).
        build() );

// Edit
configMap = client.k8s().configMaps().
    inNamespace( "my-namespace" ).
    withName( "my-config-map" ).
    edit( c -> {
        c.setData( Map.of( "my", "new-data-with-edit" ) );
        return c;
    } );

// Get
configMap = client.k8s().configMaps().
    inNamespace("my-namespace").
    withName( "my-config-map" ).
    get();

// Delete
client.k8s().configMaps().
    inNamespace("my-namespace").
    withName( "my-config-map" ).
    delete();
```

### Custom resources

> **_NOTE:_** See more details in the [tests](./src/test/java/com/enonic/kubernetes/client/CrudTest.java).

```java
// Get CRD client
MixedOperation<Xp7App, Xp7App.Xp7AppList, Resource<Xp7App>> xp7AppClient = client.enonic().v1().crds().xp7apps();

// Create
Xp7App xp7App = xp7AppClient.
    inNamespace( "my-namespace" ).
    withName( "my-app" ).
    create( new Xp7App().
        withSpec( new Xp7AppSpec().
            withUrl( "app-url" ) ) );

// Create or Replace
xp7App = xp7AppClient.
    inNamespace( "my-namespace" ).
    withName( "my-app" ).
    createOrReplace( new Xp7App().
        withSpec( new Xp7AppSpec().
            withUrl( "app-url-with-create-or-replace" ) ) );

// Patch
xp7App = xp7AppClient.
    inNamespace( "my-namespace" ).
    withName( "my-app" ).
    patch( new Xp7App().
        withSpec( new Xp7AppSpec().
            withUrl( "app-url-with-patch" ) ) );

// Replace
xp7App = xp7AppClient.
    inNamespace( "my-namespace" ).
    withName( "my-app" ).
    replace( new Xp7App().
        withSpec( new Xp7AppSpec().
            withUrl( "app-url-with-replace" ) ) );

// Edit
xp7App = xp7AppClient.
    inNamespace( "my-namespace" ).
    withName( "my-app" ).
    edit( c -> c.withSpec( new Xp7AppSpec().
        withUrl( "app-url-with-edit" ) ));

// Get
xp7App = xp7AppClient.
    inNamespace( "my-namespace" ).
    withName( "my-app" ).
    get();

// Delete
xp7AppClient.
    inNamespace( "my-namespace" ).
    withName( "my-app" ).
    delete();
```

## Watching with websockets (short lived watching)

### Standard resources

```java
// Watch
client.k8s().configMaps().
    inAnyNamespace().
    watch( new Watcher<>()
    {
        @Override
        public void eventReceived( final Action action, final ConfigMap configMap )
        {
            // On Update
        }

        @Override
        public void onClose()
        {
            // On websocket close
        }

        @Override
        public void onClose( final WatcherException e )
        {
            // On websocket exception
        }
    } );
```

### Custom resources

You can listen to events like so:

```java
// Watch
xp7AppClient.
    inAnyNamespace().
    watch( new Watcher<>()
    {
        @Override
        public void eventReceived( final Action action, final Xp7App xp7App )
        {
            // On Update
        }

        @Override
        public void onClose()
        {
            // On websocket close
        }

        @Override
        public void onClose( final WatcherException e )
        {
            // On websocket exception
        }
    } );
```

## Watching with informers (long lived watching)

### Standard resources

```java
// Get informer factory
SharedInformerFactory sharedInformerFactory = client.k8s().informers();
                                                                    
// Create informer
SharedIndexInformer<ConfigMap> configMapInformer = sharedInformerFactory.
    sharedIndexInformerFor( ConfigMap.class, 30 * 1000L );

// Add event handler
configMapInformer.addEventHandler( new ResourceEventHandler<ConfigMap>()
{
    @Override
    public void onAdd( final ConfigMap configMap )
    {
        // On add
    }

    @Override
    public void onUpdate( final ConfigMap configMap, final ConfigMap t1 )
    {
        // On update
    }

    @Override
    public void onDelete( final ConfigMap configMap, final boolean b )
    {
        // On delete
    }
} );

// Start all informers
sharedInformerFactory.startAllRegisteredInformers();

...

// Stop all informers
sharedInformerFactory.stopAllRegisteredInformers();
```

### Custom resources

```java
// Get informer factory
SharedInformerFactory sharedInformerFactory = client.k8s().informers();

// Create informer
SharedIndexInformer<Xp7App> xp7AppInformer = sharedInformerFactory.
    sharedIndexInformerFor( Xp7App.class, 30 * 1000L );

// Add event handler
xp7AppInformer.addEventHandler( new ResourceEventHandler<Xp7App>()
{
    @Override
    public void onAdd( final Xp7App xp7App )
    {
        // On add
    }

    @Override
    public void onUpdate( final Xp7App xp7App, final Xp7App t1 )
    {
        // On update
    }

    @Override
    public void onDelete( final Xp7App xp7App, final boolean b )
    {
        // On delete
    }
} );

// Start all informers
sharedInformerFactory.startAllRegisteredInformers();

...

// Stop all informers
sharedInformerFactory.stopAllRegisteredInformers();
```
