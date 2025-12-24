package com.kidtoadultai.kid_to_adult_ai.dto;

public class ImageRequest {
    private String base64Image;
    private String profession;
    private int targetAge;

    // Getters and setters
    public String getBase64Image() { return base64Image; }
    public void setBase64Image(String base64Image) { this.base64Image = base64Image; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public int getTargetAge() { return targetAge; }
    public void setTargetAge(int targetAge) { this.targetAge = targetAge; }
}
