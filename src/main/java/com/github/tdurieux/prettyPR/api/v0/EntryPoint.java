package com.github.tdurieux.prettyPR.api.v0;

import com.github.tdurieux.prettyPR.spoon.SpoonUtils;
import difflib.DiffUtils;
import difflib.Patch;
import org.kohsuke.github.*;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Path("/github")
public class EntryPoint {

    @GET
    @Path("{user}/{project}/{pr}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response analysePR(@PathParam("user") String user, @PathParam("project") String project, @PathParam("pr") int pr) {
        String content = "";
        try {
            GitHub github = GitHub.connectAnonymously();
            GHUser githubUser = github.getUser(user);
            if(githubUser == null) {
                return Response.status(404).entity("{error: 'User not found'}").build();
            }
            GHRepository repository = githubUser.getRepository(project);
            if(repository == null) {
                return Response.status(404).entity("{error: 'Repository not found'}").build();
            }
            GHPullRequest pullRequest = repository.getPullRequest(pr);
            if(pullRequest == null) {
                return Response.status(404).entity("{error: 'Pull request not found'}").build();
            }
            List<GHPullRequestFileDetail> ghPullRequestFileDetails = pullRequest.listFiles().asList();
            for (int i = 0; i < ghPullRequestFileDetails.size(); i++) {
                GHPullRequestFileDetail ghPullRequestFileDetail = ghPullRequestFileDetails.get(i);
                List<String> linesPatch = new ArrayList<String>();
                linesPatch.add("--- file");
                linesPatch.add("+++ file");
                linesPatch.addAll(strToListStr(ghPullRequestFileDetail.getPatch()));
                for (int j = 2; j < linesPatch.size(); j++) {
                    String s = linesPatch.get(j);
                    int index = s.lastIndexOf("@@");
                    if(index > 0) {
                        linesPatch.set(j, s.substring(0, index + 2));
                    }
                }
                Patch<String> stringPatch = DiffUtils.parseUnifiedDiff(linesPatch);
                String newContent = downloadFile(ghPullRequestFileDetail.getRawUrl());
                String oldContent = listStr2Str(DiffUtils.unpatch(strToListStr(newContent), stringPatch));
                if(newContent.isEmpty()) {
                    content += "Remove " + ghPullRequestFileDetail.getFilename() + "\n";
                    continue;
                }
                if(oldContent.isEmpty()) {
                    content += "Create " + ghPullRequestFileDetail.getFilename() + "\n";
                }

                try {
                    CtType<?> oldType = SpoonUtils.stringToCTElement(oldContent);
                    CtType<?> newType = SpoonUtils.stringToCTElement(newContent);
                    Patch<String> diff = DiffUtils.diff(strToListStr(oldType.toString()), strToListStr(newType.toString()));
                    content += (listStr2Str(DiffUtils.generateUnifiedDiff(ghPullRequestFileDetail.getFilename(), ghPullRequestFileDetail.getFilename(), strToListStr(oldType.toString()), diff, 1))) +"\n";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return Response.status(201).entity(content).build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.status(500).entity("Error").build();
    }

    private List<String> strToListStr(String str) {
        return Arrays.asList(str.split("\n"));
    }

    private String listStr2Str(List<String> list) {
        String content = null;
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if(content == null) {
                content = s;
            } else {
                content += "\n" + s;
            }
        }
        return content;
    }

    private String downloadFile(URL url) {
        try {
            URLConnection urlConnection = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream()));

            String content = null;
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                if(content == null) {
                    content = inputLine;
                } else {
                    content += "\n" + inputLine;
                }
            }
            br.close();
            return content;
        } catch (IOException e) {
            return null;
        }
    }
}
