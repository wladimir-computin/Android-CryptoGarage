package de.wladimircomputin.libcryptoiot.protocol;


import android.content.Context;
import android.os.Handler;

import de.wladimircomputin.libcryptoiot.R;

public class ChallengeManager {

    private String challenge = "";
    private int challenge_validity_timeout;

    private Handler handler;

    private Runnable r = () -> {
        resetChallenge();
    };

    public ChallengeManager(Context context){
        challenge_validity_timeout = context.getResources().getInteger(R.integer.challenge_validity_timeout) - 1000;
        handler = new Handler();
    }

    public void setChallenge(String last_challenge_response_b64){
        challenge = last_challenge_response_b64;
        handler.removeCallbacks(r);
        handler.postDelayed(r, challenge_validity_timeout);
    }

    public String getCurrentChallenge(){
        return challenge;
    }

    public void resetChallenge(){
        handler.removeCallbacks(r);
        challenge = "";
    }
}
