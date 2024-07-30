# Aprendiendo a usar OAuth2 con Spring Security

Este proyecto funciona junto a un cliente OAuth2 que se encuentra en el siguiente repositorio: [sacooauth-client](https://github.com/CurtoBrull/clientOauth).

## 1. Crear un proyecto Spring Boot

Para crear un proyecto Spring Boot, se puede utilizar [Spring Initializr](https://start.spring.io/) o un IDE con soporte para Spring Boot, como [Spring Tool Suite](https://spring.io/tools) o [IntelliJ IDEA](https://www.jetbrains.com/idea/).

En este caso, se creó un proyecto con las siguientes características:

- Project: Maven Project
- Language: Java
- Spring Boot: 3.3.2
- Project Metadata:
    - Group: eu.jacurtobr
    - Artifact: sacooauth
    - Name: sacooauth
    - Description: Spring Boot oauth project
    - Package name: eu.jacurtobr.sacooauth
- Dependencies:
    - Spring Web
    - Spring Security
    - OAuth2 Authorization Server
    - Spring Boot DevTools
    - Spring Boot Starter Test
    - Spring Security Test
- Java: 21
- Packaging: Jar

## 2. Application properties

En el archivo `application.properties` se deben configurar las propiedades de la aplicación. 

En este caso, simplemente se indicó el puerto en el que se ejecutará la aplicación de servidor de autorización.

```properties
server.port=9001
```

## 3. Configuración de la aplicación

Tal y como se indica en la documentación de Spring Security, se debe crear una clase de configuración que llamamos SecurityConfig.

El archivo `SecurityConfig.java` configura la seguridad para el servidor de autorización OAuth2 utilizando Spring Security. La clase está anotada con `@Configuration` y `@EnableWebSecurity`, lo que indica que es una clase de configuración para Spring Security.

### 3.1. Configuración de SecurityConfig

#### 3.1.1. authorizationServerSecurityFilterChain

El método `authorizationServerSecurityFilterChain` configura la cadena de filtros de seguridad para el servidor de autorización. Aplica la configuración de seguridad predeterminada y maneja excepciones redirigiendo a una página de inicio de sesión para solicitudes HTML.

La ruta de login se configura en `/login`.

#### 3.1.2. defaultSecurityFilterChain

El método `defaultSecurityFilterChain` configura la cadena de filtros de seguridad predeterminada para la aplicación. Está anotado con @Bean y @Order(2), lo que indica que es un bean de Spring y debe cargarse con un orden específico en la cadena de filtros de seguridad.  El método comienza configurando la seguridad HTTP para autorizar todas las solicitudes entrantes, requiriendo autenticación para cualquier solicitud:

```java
.authorizeHttpRequests(authorize -> authorize
        .anyRequest().authenticated()
)
```
Luego, desactiva la protección CSRF usando `.csrf(csrf -> csrf.disable())`. Esto se hace típicamente en escenarios donde no se necesita protección CSRF, como cuando se utilizan mecanismos de autenticación sin estado como JWT. 
Finalmente, configura el inicio de sesión basado en formularios con configuraciones predeterminadas usando `.formLogin(Customizer.withDefaults())`. Esto habilita un formulario de inicio de sesión estándar para la autenticación de usuarios. 
El método devuelve la ``SecurityFilterChain`` configurada llamando a ``http.build()``, lo que finaliza la configuración de seguridad.

### 3.2. userDetailsService

El método `userDetailsService` es un bean de Spring que configura un servicio de detalles de usuario en memoria. Este servicio se utiliza para gestionar la información y autenticación de usuarios dentro de la aplicación.

El método comienza creando un objeto `UserDetails` utilizando el método `User.builder()`. Se establece el nombre de usuario como "user" y la contraseña como "password", con la contraseña almacenada en texto plano (indicado por el prefijo `{noop}`). Al usuario se le asigna el rol "USER":

```java
UserDetails userDetails = User.builder()
        .username("user")
        .password("{noop}password")
        .roles("USER")
        .build();
```

El método devuelve una nueva instancia de `InMemoryUserDetailsManager`, inicializada con el objeto `UserDetails` creado. Este gestor manejará la autenticación de usuarios y la recuperación de detalles de usuario desde la memoria:

```java
return new InMemoryUserDetailsManager(userDetails);
```

### 3.3. registeredClientRepository

El método `registeredClientRepository` es un bean de Spring que configura un repositorio en memoria para clientes OAuth2 registrados. Este repositorio se utiliza para gestionar la información y autenticación de los clientes dentro del servidor de autorización.

Se crea un objeto `RegisteredClient` utilizando el método `RegisteredClient.withId(UUID.randomUUID().toString())`. Se establece el ID del cliente como "client-oauth" y el secreto del cliente como "secret", con el secreto almacenado en texto plano (indicado por el prefijo `{noop}`). 
El cliente se configura para usar el método de autenticación `CLIENT_SECRET_BASIC` y soporta tanto los tipos de concesión `AUTHORIZATION_CODE` como `REFRESH_TOKEN`:

```java
RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("client-oauth")
        .clientSecret("{noop}secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
```

El método configura las URIs de redirección para el cliente, que son los puntos finales a los que el servidor de autorización redirigirá después de una autenticación y autorización exitosa:

```java
.redirectUri("http://127.0.0.1:8080/login/oauth2/code/client-oauth")
.redirectUri("http://127.0.0.1:8080/authorized")
.postLogoutRedirectUri("http://127.0.0.1:8080/logout")
```

El cliente también se asigna varios alcances, incluyendo "read", "write", `OidcScopes.OPENID`, y `OidcScopes.PROFILE`, que definen los permisos que el cliente puede solicitar:

```java
.scope("read")
.scope("write")
.scope(OidcScopes.OPENID)
.scope(OidcScopes.PROFILE)
```

Se configuran los ajustes del cliente para no requerir el consentimiento del usuario para la autorización, y el método devuelve una nueva instancia de `InMemoryRegisteredClientRepository` inicializada con el objeto `RegisteredClient` creado:

```java
.clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
.build();

return new InMemoryRegisteredClientRepository(client);
```

#### 3.3.1. jwkSource

El método `jwkSource` es un bean de Spring que configura una fuente de claves JSON Web Key (JWK) con una única clave RSA. 
Este bean se utiliza para proporcionar claves criptográficas que se emplean en la firma y verificación de tokens JWT en el servidor de autorización.

Se generan un par de claves RSA mediante el método `generateRsaKey()`, que devuelve un objeto `KeyPair` que contiene una clave pública y una clave privada:

```java
KeyPair keyPair = generateRsaKey();
RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
```

Se crea un objeto `RSAKey` utilizando las claves pública y privada generadas. 
Este objeto también se le asigna un identificador único (keyID) generado aleatoriamente:

```java
RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
```

Luego, se crea un conjunto de claves JWK (`JWKSet`) que contiene la clave RSA creada:

```java
JWKSet jwkSet = new JWKSet(rsaKey);
```

El método devuelve una instancia de `ImmutableJWKSet`, una implementación inmutable de `JWKSource` que contiene el conjunto de claves JWK:

```java
return new ImmutableJWKSet<>(jwkSet);
```

Esta configuración es importante para la seguridad del servidor de autorización, ya que permite la gestión y distribución segura de claves criptográficas para la firma y verificación de tokens JWT.

#### 3.3.2. generateRsaKey

El método `generateRsaKey` es un método auxiliar que genera un par de claves RSA utilizando el algoritmo `RSA` y una longitud de clave de 2048 bits.

Se crea un generador de claves RSA (`KeyPairGenerator`) y se inicializa con una longitud de clave de 2048 bits:

```java
KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
keyPairGenerator.initialize(2048);
```

Se generan las claves RSA llamando al método `generateKeyPair()` del generador de claves, que devuelve un objeto `KeyPair` que contiene una clave pública y una clave privada:

```java
return keyPairGenerator.generateKeyPair();
```

El método devuelve el par de claves generado, que se utiliza para firmar y verificar tokens JWT en el servidor de autorización.

#### 3.3.3. jwtDecoder

El método `jwtDecoder` es un bean de Spring que configura un decodificador de tokens JWT. Este decodificador se utiliza para verificar y decodificar tokens JWT en el servidor de autorización.

Toma como parámetro un objeto `JWKSource`, que proporciona las claves criptográficas necesarias para la verificación de los tokens JWT. Este `JWKSource` se obtiene de otro método configurado en la clase, `jwkSource`.

Dentro del método, se llama a `OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)` para crear una instancia de `JwtDecoder` configurada con la fuente de claves JWK proporcionada:

```java
return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
```

Esto permite la verificación de la firma de los tokens JWT, asegurando que no hayan sido alterados y que provengan de una fuente confiable.

#### 3.3.4. authorizationServerSettings

El método `authorizationServerSettings` es un bean de Spring que configura los ajustes del servidor de autorización OAuth2. Este método devuelve una instancia de `AuthorizationServerSettings`, que contiene la configuración necesaria para el funcionamiento del servidor de autorización.

Se utiliza el constructor `AuthorizationServerSettings.builder()` para crear un objeto `AuthorizationServerSettings` con la configuración predeterminada. El método `build()` se llama para finalizar la construcción del objeto:

```java
return AuthorizationServerSettings.builder().build();
```

Define los parámetros y comportamientos del servidor de autorización, asegurando que esté configurado correctamente para manejar las solicitudes de autorización y autenticación de los clientes OAuth2.

## 4. Ejecutar la aplicación

Para ejecutar la aplicación, se puede utilizar un IDE con soporte para Spring Boot, como Spring Tool Suite o IntelliJ IDEA, o ejecutar el comando `mvn spring-boot:run` en la línea de comandos.

Una vez que la aplicación esté en funcionamiento, se puede acceder a ella en un navegador web visitando la URL `http://localhost:9001`.

Este proyecto funciona junto a un cliente OAuth2 que se encuentra en el siguiente repositorio: [sacooauth-client](https://github.com/CurtoBrull/clientOauth).