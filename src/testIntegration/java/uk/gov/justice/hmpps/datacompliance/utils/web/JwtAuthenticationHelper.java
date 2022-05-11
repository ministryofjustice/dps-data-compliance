package uk.gov.justice.hmpps.datacompliance.utils.web;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;

import static io.jsonwebtoken.SignatureAlgorithm.RS256;
import static java.util.UUID.randomUUID;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.joda.time.LocalDateTime.now;

@Component
public class JwtAuthenticationHelper {

    private final KeyPair keyPair;

    public JwtAuthenticationHelper(@Value("${jwt.signing.key.pair}") final String privateKeyPair,
                                   @Value("${jwt.keystore.password}") final String keystorePassword,
                                   @Value("${jwt.keystore.alias}") final String keystoreAlias) {
        keyPair = getKeyPair(new ByteArrayResource(decodeBase64(privateKeyPair)), keystoreAlias, keystorePassword.toCharArray());
    }

    public String createJwt() {

        final var claims = Map.<String, Object>of(
            "user_name", "data-compliance-user",
            "client_id", "data-compliance-client",
            "internalUser", true);

        return Jwts.builder()
            .setId(randomUUID().toString())
            .setSubject("data-compliance-user")
            .addClaims(claims)
            .setExpiration(now().plusDays(1).toDate())
            .signWith(RS256, keyPair.getPrivate())
            .setHeaderParam("typ", "JWT")
            .setHeaderParam("kid", "dps-client-key")
            .compact();
    }

    private KeyPair getKeyPair(final Resource resource, final String alias, final char[] password) {

        try (final InputStream inputStream = resource.getInputStream()) {

            final var store = KeyStore.getInstance("jks");

            store.load(inputStream, password);

            final var key = (RSAPrivateCrtKey) store.getKey(alias, password);
            final var spec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
            final var publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);

            return new KeyPair(publicKey, key);

        } catch (Exception e) {
            throw new IllegalStateException("Cannot load keys from store: " + resource, e);
        }
    }
}
