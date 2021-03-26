package fr.inria.clea.lsp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import fr.devnied.bitlib.BytesUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class Location {
    public static final String COUNTRY_SPECIFIC_PREFIX = "https://tac.gouv.fr/";
    private String permanentLocationSecretKey;
    private String serverAuthorityPublicKey;
    private String manualContactTracingAuthorityPublicKey;
    private CleaEciesEncoder cleaEncoder;

    @Getter
    private LocationContact contact;
    @Getter
    private LocationSpecificPart locationSpecificPart;
    
    /**
     * Create a new deep link (that can be encoded as a QR code)
     * with a period starting at the current time.
     * 
     * @return the deep link as a String
     * @throws CleaEncryptionException
     */
    public String newDeepLink() throws CleaEncryptionException {
        Instant periodStartTime = Instant.now().truncatedTo(ChronoUnit.HOURS);
        return this.newDeepLink(periodStartTime);
    }
    
    /**
     * Create a new deep link (that can be encoded as a QR code)
     * at the given period start time with a QR code validaty
     * starting at the period start time.
     * 
     * @param periodStartTime Starting time of the period. A period necessarily starts at a round hour.
     * @return the deep link as a String
     * @throws CleaEncryptionException
     */
    public String newDeepLink(Instant periodStartTime) throws CleaEncryptionException {
        // QR-code validity starts at period start time 
        return this.newDeepLink(periodStartTime, periodStartTime);
    }
    
    /**
     * Create a new deep link (that can be encoded as a QR code)
     * at the given period start time with a QR code validaty
     * starting at the period start time.
     * 
     * @param periodStartTime Starting time of the period. A period necessarily starts at a round hour.
     * @param qrCodeValidityStartTime Starting time of the QR code validity timespan.
     * @return the deep link as a String
     * @throws CleaEncryptionException
     */
    public String newDeepLink(Instant periodStartTime, Instant qrCodeValidityStartTime) throws CleaEncryptionException {
        this.setPeriodStartTime(periodStartTime);
        this.setQrCodeValidityStartTime(periodStartTime, qrCodeValidityStartTime);
        return COUNTRY_SPECIFIC_PREFIX + this.getLocationSpecificPartEncryptedBase64();
    }
    
    /**
     * Get the encrypted location specific part encoded in Base 64.
     * @return the base 64 encoded location specific part
     * @throws CleaEncryptionException
     */
    public String getLocationSpecificPartEncryptedBase64() throws CleaEncryptionException {
        return Base64.getEncoder().encodeToString(this.getLocationSpecificPartEncrypted());
    }

    protected byte[] getLocationSpecificPartEncrypted() throws CleaEncryptionException {
        if (Objects.nonNull(this.contact)) {
            this.locationSpecificPart.setEncryptedLocationContactMessage(this.getLocationContactMessageEncrypted());
        }
        return new LocationSpecificPartEncoder(this.serverAuthorityPublicKey).encode(locationSpecificPart);
    }
    
    protected byte[] getLocationContactMessageEncrypted() throws CleaEncryptionException {
        return new LocationContactMessageEncoder(this.manualContactTracingAuthorityPublicKey).encode(contact);
    }
    
    protected void setPeriodStartTime(Instant periodStartTime) throws CleaEncryptionException {
        byte[] locationTemporarySecretKey = this.getCleaEncoder().computeLocationTemporarySecretKey(this.permanentLocationSecretKey, periodStartTime);
        UUID currentLocationTemporaryPublicId = this.getCleaEncoder().computeLocationTemporaryPublicId(locationTemporarySecretKey);
        this.locationSpecificPart.setPeriodStartTime(periodStartTime);
        this.locationSpecificPart.setLocationTemporarySecretKey(locationTemporarySecretKey);
        this.locationSpecificPart.setLocationTemporaryPublicId(currentLocationTemporaryPublicId);
        if (Objects.nonNull(this.contact)) {
            this.contact.setPeriodStartTime(periodStartTime);
        }
        log.debug("new periodStartTime: {} ", periodStartTime);
        log.debug("locationTemporarySecretKey*: {}*", BytesUtils.bytesToString(locationTemporarySecretKey));
        log.debug("locationTemporaryPublicID: " + currentLocationTemporaryPublicId.toString());
    }
    
    protected void setQrCodeValidityStartTime(Instant periodStartTime, Instant qrCodeValidityStartTime) {
        if ((this.locationSpecificPart.getQrCodeRenewalInterval() == 0) 
                && Objects.nonNull(this.locationSpecificPart.getQrCodeValidityStartTime())) {
            log.warn("Cannot update QrCode validity start time. No renewal specified!");
            return;
        }

        if (qrCodeValidityStartTime.isBefore(periodStartTime)) {
            log.warn("Cannot set QrCode validity start time to {}. It preceeds period validity (start: {}, duration (in hours): {}", 
                    qrCodeValidityStartTime, periodStartTime, this.locationSpecificPart.getPeriodDuration());
            return;
        }

        if (qrCodeValidityStartTime.isAfter(periodStartTime.plus(this.locationSpecificPart.getPeriodDuration(), ChronoUnit.HOURS))) {
            log.warn("Cannot set QrCode validity start time to {}. It exceeds period validity (start: {}, duration (in hours): {}", 
                    qrCodeValidityStartTime, periodStartTime, this.locationSpecificPart.getPeriodDuration());
            return;
        }
        
        if ((this.locationSpecificPart.getQrCodeRenewalInterval() != 0)  &&
            (((qrCodeValidityStartTime.getEpochSecond() - periodStartTime.getEpochSecond()) % this.locationSpecificPart.getQrCodeRenewalInterval()) != 0)) {
            log.warn("Cannot set QrCode validity start time to {}. It is not a multiple of qrCodeRenewalInterval (qrCodeValidityStartTime: {}, periodStartTime: {}, qrCodeRenewalInterval: {}", 
                    qrCodeValidityStartTime, periodStartTime, this.locationSpecificPart.getQrCodeRenewalInterval());
            return;
        }
        
        this.locationSpecificPart.setQrCodeValidityStartTime(qrCodeValidityStartTime);
    }
    
    protected CleaEciesEncoder getCleaEncoder() {
        return Objects.isNull(this.cleaEncoder) ? this.cleaEncoder = new CleaEciesEncoder() : this.cleaEncoder;
    }
}
