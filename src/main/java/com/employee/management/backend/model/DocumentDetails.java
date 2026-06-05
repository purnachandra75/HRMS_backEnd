package com.employee.management.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "document_details")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DocumentDetails {
    @Id
    private Long empId;

    private String resumeUpload;
    private String idProofUpload;
    private String addressProofUpload;
    private String educationalCertificates;
    private String experienceCertificates;
    private String passportPhoto;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "emp_id")
    @JsonIgnore
    private Employee employee;

    public Long getEmpId() {
        return empId;
    }

    public void setEmpId(Long empId) {
        this.empId = empId;
    }

    public String getResumeUpload() {
        return resumeUpload;
    }

    public void setResumeUpload(String resumeUpload) {
        this.resumeUpload = resumeUpload;
    }

    public String getIdProofUpload() {
        return idProofUpload;
    }

    public void setIdProofUpload(String idProofUpload) {
        this.idProofUpload = idProofUpload;
    }

    public String getAddressProofUpload() {
        return addressProofUpload;
    }

    public void setAddressProofUpload(String addressProofUpload) {
        this.addressProofUpload = addressProofUpload;
    }

    public String getEducationalCertificates() {
        return educationalCertificates;
    }

    public void setEducationalCertificates(String educationalCertificates) {
        this.educationalCertificates = educationalCertificates;
    }

    public String getExperienceCertificates() {
        return experienceCertificates;
    }

    public void setExperienceCertificates(String experienceCertificates) {
        this.experienceCertificates = experienceCertificates;
    }

    public String getPassportPhoto() {
        return passportPhoto;
    }

    public void setPassportPhoto(String passportPhoto) {
        this.passportPhoto = passportPhoto;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
