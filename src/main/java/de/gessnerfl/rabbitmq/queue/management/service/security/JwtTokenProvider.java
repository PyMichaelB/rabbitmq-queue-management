package de.gessnerfl.rabbitmq.queue.management.service.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class JwtTokenProvider {

    private final JWTConfig jwtConfig;
    private final String secretKeyBase64Encoded;
    private final JWSAlgorithm jwsAlgorithm;

    public JwtTokenProvider(JWTConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKeyBase64Encoded = jwtConfig.getToken().getSecretKeyBase64Encoded();
        var keyLength = secretKeyBase64Encoded.length();
        this.jwsAlgorithm = keyLength < 48 ? JWSAlgorithm.HS256 : (keyLength < 64 ? JWSAlgorithm.HS384 : JWSAlgorithm.HS512);
    }

    public String createToken(UserDetails user) {
        try {
            Date now = new Date();
            Date expirationTime = new Date(now.getTime() + jwtConfig.getToken().getValidity().toMillis());
            JWSSigner signer = new MACSigner(secretKeyBase64Encoded);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getUsername())
                    .issuer(jwtConfig.getToken().getIssuer())
                    .audience(jwtConfig.getToken().getAudience())
                    .issueTime(now)
                    .expirationTime(expirationTime)
                    .claim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(jwsAlgorithm), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        }catch (JOSEException e){
            throw new JwtTokenCreationFailedException("Failed to create JWT token", e);
        }
    }

    public UserDetails getUserDetailsFromToken(String token) {
        try {
            var jwt = parseAndVerifyToken(token);
            var claimsSet = jwt.getJWTClaimsSet();
            return new User(claimsSet.getSubject(), "", claimsSet.getStringListClaim("roles").stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
        } catch (ParseException e) {
            throw new InvalidJwtTokenException("Failed to parse claims from JWT token", e);
        }
    }

    private SignedJWT parseAndVerifyToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            verifySignature(jwt);
            verifyClaims(jwt);
            return jwt;
        }catch (ParseException | JOSEException | BadJWTException e) {
            throw new BadCredentialsException("Expired or invalid JWT token", e);
        }
    }

    private void verifySignature(SignedJWT jwt) throws JOSEException {
        JWSVerifier verifier = new MACVerifier(secretKeyBase64Encoded);
        if(!jwt.verify(verifier)){
            throw new BadCredentialsException("Signature of JWT token not valid");
        }
    }

    private void verifyClaims(SignedJWT jwt) throws ParseException, BadJWTException {
        String issuer = jwtConfig.getToken().getIssuer();
        String audience = jwtConfig.getToken().getAudience();
        var verifier = new DefaultJWTClaimsVerifier(audience, new JWTClaimsSet.Builder().issuer(issuer).build(), new HashSet<>(Arrays.asList("exp", "sub", "roles")));
        verifier.verify(jwt.getJWTClaimsSet());
    }
}
