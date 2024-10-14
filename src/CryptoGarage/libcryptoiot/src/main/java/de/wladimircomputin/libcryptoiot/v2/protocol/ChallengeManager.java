package de.wladimircomputin.libcryptoiot.v2.protocol;


import static de.wladimircomputin.libcryptoiot.v2.Constants.challenge_validity_timeout;

import android.os.Handler;

import java.security.SecureRandom;
import java.util.Arrays;

public class ChallengeManager {
    public static final int CHALLENGE_LEN = 12;
    private byte[] last_challenge_request = new byte[CHALLENGE_LEN];
    private byte[] next_challenge_response = new byte[CHALLENGE_LEN];
    private int timeout;
    private SecureRandom random;

    private Handler handler;

    private Runnable r = () -> {
        resetChallengeResponse();
    };

    public ChallengeManager(){
        timeout = challenge_validity_timeout - 1000;
        handler = new Handler();
        this.random = new SecureRandom();
    }

    public byte[] generateRandomChallengeRequest() {
        random.nextBytes(last_challenge_request);
        return last_challenge_request;
    }

    public byte[] getCurrentChallengeRequest(){
        return last_challenge_request;
    }

    public void setNextChallengeResponse(byte[] challenge){
        this.next_challenge_response = challenge;
        handler.removeCallbacks(r);
        handler.postDelayed(r, timeout);
    }

    public byte[] getNextChallengeResponse(){
        return next_challenge_response;
    }

    public void resetChallengeResponse(){
        handler.removeCallbacks(r);
        next_challenge_response = new byte[CHALLENGE_LEN];
    }

    public boolean verifyChallenge(byte[] challenge_response){
        return Arrays.equals(this.last_challenge_request, challenge_response);
    }

    public boolean hasNextChallengeResponse(){
        return !Arrays.equals(this.next_challenge_response, new byte[CHALLENGE_LEN]);
    }
}
