
# Android-CryptoGarage
ESP8266/Android based smartphone garage door opener. Focused on security. Repo for client software (Android / Wear)) 

## Encryption scheme ##
![Networkcommunication between client and server](https://github.com/wladimir-computin/Android-CryptoGarage/raw/master/doc/Flow.png)

### Explanations ###
#### Q: Why SHA512 is used for key generation instead of sCrypt, bCrypt or PBKDF2? ####
There was no tested implementation of those algorithms for Arduino/ESP8266. I'm using the Arduino Cryptographic Library https://rweather.github.io/arduinolibs/crypto.html for anything crypto-related except base64.  Also, 5000 rounds of SHA512 + a fixed random salt (protection against rainbow tables if user selects a weak password) should be enough to slow down attacks without having a noticeable impact on performance.

#### Q: Why 5000 rounds of SHA512 and not more (or less)? ####
My ESP8266 performs the key generation from the passphrase at boot time. I measured the time it takes to generate the key with different iteration values and got a delay of about one second for 5000 iterations. Greater values may trigger the internal watchdogs https://www.sigmdel.ca/michel/program/esp8266/arduino/watchdogs_en.html and cause delays on older Android devices. Selecting a solid password will provide enough security for most needs.

### Possible Attacks ###
#### Attacking the ClientHello message ####
This message has always the same payload and is encrypted using the same password. That's no problem since AES256-GCM with random IVs is used. However, this packet can be eavesdropped and is an ideal candidate for offline brute-forcing in order to break the password. I did some calculations to determine the required time for an successful attack in dependence of the password length.

Assuming a **8x GTX1080** setup with **Hashcat 3.00*** (see https://gist.github.com/epixoip/a83d38f412b4737e99bbef804a270c40) and that AES-GCM is incredibly fast so **only the SHA512 password hashing** is relevant.

hashrate (SHA512, 1 round): 8624.7 MH/s
hashrate (SHA512, 5000 rounds, approx.): 1.725 MH/s

#### Time to bruteforce the whole key space: ####
| Password length | 8 | 10 | 12 | 14
|--|--:|--:|--:|--:|
| **lowercase (26)** | 1.4 days | 2.6 years | 1 754 years | 1 185 853 years |
| **lower + upper (52)** | 358.7 days | 2 657 years | 7 185 291 years | very long |
| **alphanumeric (62)**| 4 years | 15 428 years | very long | ridiculously long |

Since you set the device password only once and never have to type it again, just use a proper password length depending on how paranoid you are.

#### Opening the garage with explosives ####
Well, that's a hardware problem ;)
