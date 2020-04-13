# mTLS Sample

This project demonstrates mTLS authentication and authorization when running on Cloud Foundry and SSL is terminated at container level.  This sample has both server and applications that demonstrate the configuration required to use mTLS as well as log output documenting the use of mTLS.

**Note:** This forked project demonstrates SSL Termination at application level by using Cloud foundry container to container networking and container instance identity.  

## Generate Keystore and TrustStore for mtl-sample-server

The following commands are executed in the directory.

```shell
$ cd server/cert
```
#####Get the certificate authority (CA) public key (certificate) and Private Key
Get the instance_identity_ca.key, instance_identity_ca.cert, root certificate of instance_identity_ca.cert from dev ops/cloud ops team and copy the files to the directory. 

Note: In our company cloud foundry development space, normally these certificates (instance_identity_ca.cert, root certificate of instance_identity_ca.cert) are available at $CF_SYSTEM_CERT_PATH  (/etc/cf-system-certificates). 
You could copy this file from one of the existing cloud foundry app via ssh shell. For example,
```shell
cf ssh <CF APP  Name> --command "cat $CF_SYSTEM_CERT_PATH/trusted-ca-1.crt" > trusted-ca-1.crt
cf ssh <CF APP  Name> --command "cat $CF_SYSTEM_CERT_PATH/trusted-ca-4.crt" > instance_identity_ca.crt 
```
For more information read https://docs.cloudfoundry.org/devguide/deploy-apps/instance-identity.html
##### Generate SSL Keys and certificates for mtls-sample-server.

```shell
$ keytool \
  -genkey \
  -keystore server.keystore \
  -alias mtls-sample-server \
  -dname CN=mtls-sample-server.apps.internal \
  -keyalg RSA \
  -validity 365 \
  -ext san=dns:mtls-sample-server.apps.internal \
  -storepass 123456
```
Note:

Subject Alternative Name (SAN) is required for mTLS authentication. So use the command line option san=dns:mtls-sample-server.apps.internal. mtls-sample-server.apps.internal is the host name for the cloud events service in apps.internal domain
-keystore server.keystore - specifies the generated keystore name. You could use any name.

mtls-sample-server is used as the -alias parameter. 
123456 is used as the keystore password. Use your own strong password.
Validity set to one year.

##### Sign Service Certificate using CA 
Export the unsigned server certificate from server. keystore and create a certificate signing request (CSR).
```shell
$ keytool \
  -certreq \
  -keystore server.keystore \
  -alias cloud-events-service \
  -file server.unsigned.crt \
  -storepass 123456
```
Sign the certificate signing request (server.unsigned.crt) with the instance identity CA.
```shell
$ openssl x509 \
  -req \
  -CA instance_identity_ca.cert \
  -CAkey instance_identity_ca.key \
  -in server.unsigned.crt \
  -out server.crt \
  -days 365 \
  -CAcreateserial \
  -passin pass:123456
```
Now you should have the following files in the directory:

```plain
server.unsigned.crt
instance_identity_ca.srl
server.crt
```

##### Import Certificates to Server Keystore
Import the CA certificate - instance_identity_ca.cert into the server keystore
```shell
$ keytool \
  -import \
  -file instance_identity_ca.cert \
  -keystore server.keystore \
  -alias instanceIdentityCA \
  -storepass 123456 \
  -noprompt  
```

Import the root certificate - trusted-ca-1.crt into the server keystore
```shell
$ keytool \
  -import \
  -file trusted-ca-1.crt \
  -keystore server.keystore \
  -alias appRootCA \
  -storepass 123456 \
  -noprompt
```

Import the signed certificate - server.crt into server keystore. Make sure to use the same -alias used earlier.
```shell
$ keytool \
  -import \
  -file server.crt \
  -keystore server.keystore \
  -alias cloud-events-service \
  -storepass 123456 \
  -noprompt
```
Use keytool to print out the certificates in the keystore. There should 3 entries.
```shell
$ keytool -list -v -keystore server.keystore -storepass 123456
```
  
####Import CA Certificate to Server Truststore

Import the CA certificate - instance_identity_ca.cert into the server truststore.
```shell
$ keytool \
    -import \
    -file instance_identity_ca.cert \
    -keystore server.truststore \
    -alias ca \
    -storepass 123456 \
    -noprompt
```

#### Base64 encode the key store and trust store
```shell  
$ base64 server.keystore > server.keystore.encoded
$ base64 server.truststore > server.truststore.encoded
```

#### Update server manifest.yml file 
Copy the base64 encoded the key store(server.keystore.encoded) file content to KEYSTORE environment variable
Copy the base64 encoded the key store(server.truststore.encoded) file content to TRUSTSTORE environment variable


## Update  
 
## Trusted Certificate, Authorized Application
This project consists of two applications, a `server` and a `client`.  To properly deploy and configure them, do the following.

```shell
$ ./mvnw clean package

$ cd server
$ cf push mtls-sample-server -s cflinuxfs3 --no-start
$ cf map-route mtls-sample-server apps.internal --hostname mtls-sample-server

$ cd ../client
$ cf push mtls-sample-client -s cflinuxfs3 --no-start
$ cf add-network-policy mtls-sample-client --destination-app mtls-sample-server --protocol tcp --port 8443
$ cf app mtls-sample-client --guid

```

The result of this final command is the client's application id.  This must be configured in the server as an "admin" client id allowing access to the `/admin` endpoint.

```shell
$ cf set-env mtls-sample-server MTLS.ADMIN-CLIENT-IDS <CLIENT_GUID>
$ cf start mtls-sample-server
```

At this point, the server has started and is configured to allow calls to `/admin` from the client application.  In the output of this final command is the server's host name (`urls`).  This must be configured in the client as the server route.

```shell
$ cf set-env mtls-sample-client MTLS.SERVER-ROUTE mtls-sample-server.apps.internal:8443
$ cf start mtls-sample-client
```

At this point the client has started and will being calling the `/` and `/admin` endpoints every five minutes.  You'll see the following client and server output.

```plain
Requesting /admin with certificate SN 266754964882990302904004562024130247468
You authenticated using x509 certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 266754964882990302904004562024130247468
Requesting / with certificate SN 266754964882990302904004562024130247468
You authenticated using x509 certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 266754964882990302904004562024130247468
```

```plain
Received request for /admin with certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 266754964882990302904004562024130247468
Received request for / with certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 266754964882990302904004562024130247468
```

If you wait long enough that the container rotates its identity (certificate and private key), you'll expect to see the same application id used, but a different serial number on the certificate.

```plain
Requesting /admin with certificate SN 266754964882990302904004562024130247468
You authenticated using x509 certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 266754964882990302904004562024130247468
Requesting / with certificate SN 266754964882990302904004562024130247468
You authenticated using x509 certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 266754964882990302904004562024130247468
Updated KeyManager for /etc/cf-instance-credentials/instance.key and /etc/cf-instance-credentials/instance.crt
Updated KeyManager for /etc/cf-instance-credentials/instance.key and /etc/cf-instance-credentials/instance.crt
Requesting /admin with certificate SN 317113556697541063389275859994730153678
You authenticated using x509 certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 317113556697541063389275859994730153678
Requesting / with certificate SN 317113556697541063389275859994730153678
You authenticated using x509 certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with SN 317113556697541063389275859994730153678
```

```plain
Received request for /admin with certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with serial number 266754964882990302904004562024130247468
Received request for / with certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with serial number 266754964882990302904004562024130247468
Received request for /admin with certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with serial number 317113556697541063389275859994730153678
Received request for / with certificate for app:a15d127a-621e-4b5f-beed-ce2dfa0763ea with serial number 317113556697541063389275859994730153678
```

## Trusted Certificate, Unauthorized Application

To demonstrate the rejection of a trusted certificate, push the same client to a different application that isn't configured in the server.

```shell
$ cf push mtls-sample-client-2 -s cflinuxfs3 --no-start
$ cf add-network-policy mtls-sample-client-2 --destination-app mtls-sample-server --protocol tcp --port 8443

$ cf set-env mtls-sample-client-2 MTLS.SERVER-ROUTE mtls-sample-server.apps.internal:8443
$ cf start mtls-sample-client-2
```

At this point the second lient has started, and is receiving a rejection from the `/admin` endpoint for being unauthorized

```plain
Requesting /admin with certificate SN 239602280072486236703492394465041616501
Received response with status code 403
Requesting / with certificate SN 239602280072486236703492394465041616501
You authenticated using x509 certificate for app:bc01e56b-e798-44b5-b992-2f3e57272c46 with SN 239602280072486236703492394465041616501
```


## Cloud Foundry Configuration

## License
This project released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).