package com.mechalikh.pureedgesim.taskgenerator;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;

public class User {

    /**
	 * The type associated to the User
	 */
    private int type;

    /**
	 * The access pattern associated to this User
	 */
    private String accessPattern;

    /**
	 * The rate at which this users makes requests
	 */
    private int rate;

    /**
	 * The start (in minutes) at which the user will make the first request
	 */
    private int start;

    /**
	 * for how long the user will make requests
	 */
    private double duration;

    /**
	 * The interval between each session of requests made from the user
	 */
    private int interval;

    /**
	 * wether or not the user is deployed on a client
	 */
    private boolean isDeployed;

    /**
	 * the computing node associated to this user
	 */
    private ComputingNode Node;

    public User(){
    }

    public User(int type, String accessPattern, int rate, int start, double duration, int interval){
        setType(type);
        setAccessPatter(accessPattern);
        setRate(rate);
        setStart(start);
        setDuration(duration);
        setInterval(interval);
        setIsDeployed(false);
    }

    public User(User user){
        setType(user.getType());
        setAccessPatter(user.getAccessPatter());
        setRate(user.getRate());
        setStart(user.getStart());
        setDuration(user.getDuration());
        setInterval(user.getInterval());
        setIsDeployed(false);
    }

    public void setComputingNode(ComputingNode computingNode){
        this.Node = computingNode;
    }

    public ComputingNode getComputingNode(){
        return this.Node;
    }

    public void setIsDeployed(boolean isDeployed){
        this.isDeployed = isDeployed;
    }

    public boolean isDeployed(){
        return this.isDeployed;
    }

    public void setType(int type){
        this.type = type;
    }

    public int getType(){
        return this.type;
    }

    public void setRate(int rate){
        this.rate = rate;
    }

    public int getRate(){
        return this.rate;
    }

    public void setDuration(double duration){
        this.duration = duration;
    }

    public double getDuration(){
        return duration;
    }

    public void setInterval(int interval){
        this.interval = interval;
    }

    public int getInterval(){
        return this.interval;
    }

    public void setStart(int start){
        this.start = start;
    }

    public int getStart(){
        return this.start;
    }

    public void setAccessPatter(String accessPattern){
        this.accessPattern = accessPattern;
    }

    public String getAccessPatter(){
        return this.accessPattern;
    }

}
