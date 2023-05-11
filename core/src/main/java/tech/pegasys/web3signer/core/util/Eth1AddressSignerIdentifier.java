package tech.pegasys.web3signer.core.util;


import tech.pegasys.signers.secp256k1.api.SignerIdentifier;

import java.security.interfaces.ECPublicKey;
import java.util.Locale;
import java.util.Objects;

import static org.web3j.crypto.Keys.getAddress;
import static tech.pegasys.signers.secp256k1.EthPublicKeyUtils.toHexString;
import static tech.pegasys.signers.secp256k1.api.util.AddressUtil.remove0xPrefix;

public class Eth1AddressSignerIdentifier implements SignerIdentifier {

    private final String address;

    public Eth1AddressSignerIdentifier(final String address) {
        this.address = remove0xPrefix(address).toLowerCase(Locale.US);
    }

    public static SignerIdentifier fromPublicKey(final String publicKey) {
        return new Eth1AddressSignerIdentifier(getAddress(publicKey));
    }

    @Override
    public String toStringIdentifier() {
        return address;
    }

    @Override
    public boolean validate(final ECPublicKey publicKey) {
        if (publicKey == null) {
            return false;
        }
        return address.equalsIgnoreCase(remove0xPrefix(getAddress(toHexString(publicKey))));
    }

    @Override
    public String toString() {
        return toStringIdentifier();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Eth1AddressSignerIdentifier that = (Eth1AddressSignerIdentifier) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}

