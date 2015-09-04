package com.github.tdurieux.prettyPR.github;

import org.kohsuke.github.GitHub;

import java.io.IOException;

public class GithubUtil {
    private static GitHub gitHub = null;

    public static GitHub getConntection() {
        if(gitHub == null) {
            try {
                gitHub = GitHub.connectAnonymously();
            } catch (IOException e) {
                throw new RuntimeException("Unable to connect github", e);
            }
        }
        return gitHub;
    }
}
