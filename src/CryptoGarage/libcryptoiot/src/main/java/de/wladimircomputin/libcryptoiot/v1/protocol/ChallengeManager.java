package de.wladimircomputin.libcryptoiot.v1.protocol;


import static de.wladimircomputin.libcryptoiot.v1.Constants.challenge_validity_timeout;

import android.os.Handler;

public class ChallengeManager {

    private String challenge = "";
    private int timeout;

    private Handler handler;

    private Runnable r = () -> {
        resetChallenge();
    };

    public ChallengeManager(){
        timeout = challenge_validity_timeout - 1000;
        handler = new Handler();
    }

    public void setChallenge(String last_challenge_response_b64){
        challenge = last_challenge_response_b64;
        handler.removeCallbacks(r);
        handler.postDelayed(r, timeout);
    }

    public String getCurrentChallenge(){
        return challenge;
    }

    public void resetChallenge(){
        handler.removeCallbacks(r);
        challenge = "";
    }
}
