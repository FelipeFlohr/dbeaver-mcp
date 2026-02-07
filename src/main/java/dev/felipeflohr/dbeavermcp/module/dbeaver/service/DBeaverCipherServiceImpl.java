package dev.felipeflohr.dbeavermcp.module.dbeaver.service;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.dbeaver.model.auth.DBeaverAuthConnectionDataDTO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@NullMarked
@RequiredArgsConstructor
@Service
class DBeaverCipherServiceImpl implements DBeaverCipherService {
    private final ObjectMapper objectMapper;
    private final DBeaverFilesService filesService;

    @Value("${dbeavermcp.dbeaver.cipher.key}")
    private String key;

    @Value("${dbeavermcp.dbeaver.cipher.iv}")
    private String iv;

    @Override
    public Map<String, DBeaverAuthConnectionDataDTO> getConnectionsAuthentication() throws DBeaverMCPValidationException {
        byte[] credentialsFileContent = readCredentialsFileAsBytes(filesService.getCredentialsConfigFilePath());
        var keySpec = new SecretKeySpec(hexToBytes(key), "AES");
        var ivSpec = new IvParameterSpec(hexToBytes(iv));

        String credentialsJson;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedBytes = cipher.doFinal(credentialsFileContent);

            int skipAfterDecrypt = 16;
            if (decryptedBytes.length <= skipAfterDecrypt) {
                throw new DBeaverMCPValidationException("Decrypted content is too short.");
            }

            credentialsJson = new String(
                    decryptedBytes,
                    skipAfterDecrypt,
                    decryptedBytes.length - skipAfterDecrypt,
                    StandardCharsets.UTF_8
            ).trim();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException
                 | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new DBeaverMCPValidationException("Not possible to decrypt credentials.", e);
        }

        try {
            var type = new TypeReference<Map<String, DBeaverAuthConnectionDataDTO>>() {};
            return objectMapper.readValue(credentialsJson, type);
        } catch (JacksonException e) {
            throw new DBeaverMCPValidationException("Not possible to read \"%s\" as a JSON".formatted(credentialsJson), e);
        }
    }

    private byte[] readCredentialsFileAsBytes(String credentialsFilePath) throws DBeaverMCPValidationException {
        try {
            return Files.readAllBytes(Paths.get(credentialsFilePath));
        } catch (IOException e) {
            throw new DBeaverMCPValidationException("Could not read credentials file.", e);
        } catch (InvalidPathException e) {
            throw new DBeaverMCPValidationException("Failed to make \"%s\" a path.".formatted(credentialsFilePath), e);
        }
    }

    private byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
}
