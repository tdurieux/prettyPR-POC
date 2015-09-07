package com.github.tdurieux.prettyPR.diff;

import com.github.tdurieux.prettyPR.github.GithubUtil;
import com.github.tdurieux.prettyPR.spoon.SpoonUtils;
import difflib.DiffUtils;
import difflib.Patch;
import org.kohsuke.github.*;
import spoon.reflect.declaration.CtType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.tdurieux.prettyPR.diff.StringUtil.downloadFile;
import static com.github.tdurieux.prettyPR.diff.StringUtil.listStr2Str;
import static com.github.tdurieux.prettyPR.diff.StringUtil.strToListStr;

public class PrettyPR {
    private String user;
    private String repository;
    private int pullRequestID;
    private Map<String, String> oldFileContent;
    private Map<String, String> newFileContent;
    private Map<String, Patch<String>> filePatch;
    private Map<String, String> fileStringPatch;
    private Map<String, String> semanticPatch;
    private Map<String, CtType> oldTypes;
    private Map<String, CtType> newTypes;
    private GHRepository ghRepository;
    private GHPullRequest pullRequest;

    public PrettyPR(final String user, final String repository, final int pullRequestID) {
        this.user = user;
        this.repository = repository;
        this.pullRequestID = pullRequestID;
        oldFileContent = new HashMap<String, String>();
        newFileContent = new HashMap<String, String>();
        filePatch = new HashMap<String, Patch<String>>();
        fileStringPatch = new HashMap<String, String>();
        semanticPatch = new HashMap<String, String>();
        oldTypes = new HashMap<String, CtType>();
        newTypes = new HashMap<String, CtType>();
        initGithub();
    }

    private void initGithub() {
        try {
            ghRepository = GithubUtil.getConntection().getRepository(user + "/" + repository);
        } catch (IOException e) {
            throw new RuntimeException("Unable to get the repository", e);
        }
        try {
            pullRequest = ghRepository.getPullRequest(pullRequestID);
        } catch (IOException e) {
            throw new RuntimeException("Unable to get the pull request", e);
        }
    }

    public void run() {
        final ExecutorService executorService = Executors.newWorkStealingPool();
        final PagedIterable<GHPullRequestFileDetail> ghPullRequestFileDetails = pullRequest.listFiles();
        for (PagedIterator<GHPullRequestFileDetail> iterator = ghPullRequestFileDetails.iterator(); iterator.hasNext(); ) {
            final GHPullRequestFileDetail changedFile = iterator.next();
            executorService.execute(new Runnable() {
                public void run() {
                    fileStringPatch.put(changedFile.getFilename(), changedFile.getPatch());
                    // the patch splited line by line
                    List<String> linesPatch = formatPatch(changedFile.getPatch());
                    // the patch parsed into a Patch
                    Patch<String> stringPatch = DiffUtils.parseUnifiedDiff(linesPatch);
                    filePatch.put(changedFile.getFilename(), stringPatch);
                    // get the content of the changed file
                    String newContent = downloadFile(changedFile.getRawUrl());
                    // undo the change in order to get the file before the change
                    String oldContent = listStr2Str(DiffUtils.unpatch(strToListStr(newContent), stringPatch));
                    oldFileContent.put(changedFile.getFilename(), oldContent);
                    newFileContent.put(changedFile.getFilename(), newContent);
                    // get the spoon type of each version
                    /*CtType<?> oldType = null;
                    if(oldContent!=null && !oldContent.isEmpty()) {
                        oldType = SpoonUtils.stringToCTElement(oldContent);
                        oldTypes.put(changedFile.getFilename(), oldType);
                    }
                    CtType<?> newType = null;
                    if(newContent!=null && !newContent.isEmpty()) {
                        newType = SpoonUtils.stringToCTElement(newContent);
                        oldTypes.put(changedFile.getFilename(), newType);
                    }
                    if(newType!= null && oldType != null) {
                        // performs the semantic diff
                        Patch<String> diff = DiffUtils.diff(strToListStr(oldType.toString()), strToListStr(newType.toString()));
                        semanticPatch.put(changedFile.getFilename(), (listStr2Str(DiffUtils.generateUnifiedDiff(changedFile.getFilename(), changedFile.getFilename(), strToListStr(oldType.toString()), diff, 0))));
                    }*/
                    System.out.println("End " + changedFile.getFilename());
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
            oldTypes = SpoonUtils.stringToCTElement(oldFileContent);
            newTypes = SpoonUtils.stringToCTElement(newFileContent);
        } catch (InterruptedException e) {

        }
        /*for (PagedIterator<GHPullRequestFileDetail> iterator = ghPullRequestFileDetails.iterator(); iterator.hasNext(); ) {
            GHPullRequestFileDetail changedFile = iterator.next();
            CtType<?> oldType = oldTypes.get(changedFile.getFilename());
            CtType<?> newType = newTypes.get(changedFile.getFilename());
            if(newType!= null && oldType != null) {
                // performs the semantic diff
                Patch<String> diff = DiffUtils.diff(strToListStr(oldType.toString()), strToListStr(newType.toString()));
                semanticPatch.put(changedFile.getFilename(), (listStr2Str(DiffUtils.generateUnifiedDiff(changedFile.getFilename(), changedFile.getFilename(), strToListStr(oldType.toString()), diff, 0))));
            }
        }*/
    }

    private List<String> formatPatch(String patch) {
        List<String> linesPatch = new ArrayList<String>();
        // add the file change
        linesPatch.add("--- file");
        linesPatch.add("+++ file");
        // add the content of the patch
        linesPatch.addAll(strToListStr(patch));
        // remove the context information in the github chuck declaration
        for (int j = 2; j < linesPatch.size(); j++) {
            String s = linesPatch.get(j);
            int index = s.lastIndexOf("@@");
            if(index > 0) {
                linesPatch.set(j, s.substring(0, index + 2));
            }
        }

        return linesPatch;
    }


    public String getUser() {
        return user;
    }

    public String getRepository() {
        return repository;
    }

    public int getPullRequestID() {
        return pullRequestID;
    }

    public Map<String, String> getOldFileContent() {
        return oldFileContent;
    }

    public Map<String, String> getNewFileContent() {
        return newFileContent;
    }

    public Map<String, Patch<String>> getFilePatch() {
        return filePatch;
    }

    public Map<String, String> getFileStringPatch() {
        return fileStringPatch;
    }

    public Map<String, String> getSemanticPatch() {
        return semanticPatch;
    }

    public Map<String, CtType> getOldTypes() {
        return oldTypes;
    }

    public Map<String, CtType> getNewTypes() {
        return newTypes;
    }

    public GHPullRequest getPullRequest() {
        return pullRequest;
    }
}
