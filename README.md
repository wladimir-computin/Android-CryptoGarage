
# Android-CryptoGarage
ESP8266/Android based smartphone garage door opener. Focused on security. Repo for client software (Android / Wear)) 

## Encryption scheme ##
![Networkcommunication between client and server](https://github.com/wladimir-computin/Android-CryptoGarage/raw/master/doc/Flow.png)

### Explanations ###
#### Q: Why SHA512 is used for key generation instead of sCrypt, bCrypt or PBKDF2? ####
There was no tested implementation of those algorithms for Arduino/ESP8266. I'm using the Arduino Cryptographic Library https://rweather.github.io/arduinolibs/crypto.html for anything crypto-related except base64.  Also, 5000 rounds of SHA512 + a fixed random salt (protection against rainbow tables if user selects a weak password) should be enough to slow down attacks without having a noticeable impact on performance.

#### Q: Why 5000 rounds of SHA512 and not more (or less)? ####
My ESP8266 performs the key generation from the passphrase at boot time. I measured the time it takes to generate the key with different iteration values and got a delay of about one second for 5000 iterations. Greater values may trigger the internal watchdogs https://www.sigmdel.ca/michel/program/esp8266/arduino/watchdogs_en.html and cause delays on older Android devices. Selecting a solid password will provide enough security for most needs.

#### Q: Why are you transmitting a random IV instead of a new random password for the challenge part? ####
Instead of sending an encrypted random Challenge_IV to the client, we could also just send an encrypted random password, which the client would decrypt and use for its next packet. The problem is, the implementation would be highly dependent on the RNG of the server-side. Random is hard, especially for embedded devices like the ESP8266. I'm using the hardware RNG of the ESP8266 (ESP True Random) which seems fine but is closed-source and if an attacker could guess (or influence) the outcome, the Challenge could be broken. By using an IV as Challenge, the password is never transmitted over the air and even with decreased randomness, AES-GCM with a solid user password will work just fine. In fact, AES-GCM doesn't even mind non-random or predictable IVs as long as they don't repeat. As we transmit only a limited amount of packets and the server-side has a rate limit, repeating IVs are very very unlikely.

### Possible Attacks ###
#### Attacking the ClientHello message ####
This message has always the same payload and is encrypted using the same password. That's no problem since AES256-GCM with random IVs is used. However, this packet can be eavesdropped and is an ideal candidate for offline brute-forcing in order to break the password. I did some calculations to determine the required time for an successful attack in dependence of the password length.

#### Guessing the Command message ####
The server has an internal state machine which decides the types of packets the server accepts. Therefore, an attacker has only one guess and only two seconds time for forging the "Command" message. Then the Challenge_IV becomes invalid and the server switches back to its initial state waiting for a ClientHello. All messages are authenticated with an AES-GCM authentication tag, so altering packets from a man-in-the-middle position without knowing the correct password (and IV) is not possible.

#### Opening the garage with explosives ####
Well, that's a hardware problem ;)
