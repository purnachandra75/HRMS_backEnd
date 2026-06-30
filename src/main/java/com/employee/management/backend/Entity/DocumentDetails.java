package com.employee.management.backend.Entity;

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

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] resumeUploadData;
    private String resumeUploadName;
    private String resumeUploadContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] idProofUploadData;
    private String idProofUploadName;
    private String idProofUploadContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] addressProofUploadData;
    private String addressProofUploadName;
    private String addressProofUploadContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] educationalCertificatesData;
    private String educationalCertificatesName;
    private String educationalCertificatesContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] experienceCertificatesData;
    private String experienceCertificatesName;
    private String experienceCertificatesContentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    @Column(columnDefinition = "LONGBLOB")
    private byte[] passportPhotoData;
    private String passportPhotoName;
    private String passportPhotoContentType;

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

    public byte[] getResumeUploadData() {
        return resumeUploadData;
    }

    public void setResumeUploadData(byte[] resumeUploadData) {
        this.resumeUploadData = resumeUploadData;
    }

    public String getResumeUploadName() {
        return resumeUploadName;
    }

    public void setResumeUploadName(String resumeUploadName) {
        this.resumeUploadName = resumeUploadName;
    }

    public String getResumeUploadContentType() {
        return resumeUploadContentType;
    }

    public void setResumeUploadContentType(String resumeUploadContentType) {
        this.resumeUploadContentType = resumeUploadContentType;
    }

    public byte[] getIdProofUploadData() {
        return idProofUploadData;
    }

    public void setIdProofUploadData(byte[] idProofUploadData) {
        this.idProofUploadData = idProofUploadData;
    }

    public String getIdProofUploadName() {
        return idProofUploadName;
    }

    public void setIdProofUploadName(String idProofUploadName) {
        this.idProofUploadName = idProofUploadName;
    }

    public String getIdProofUploadContentType() {
        return idProofUploadContentType;
    }

    public void setIdProofUploadContentType(String idProofUploadContentType) {
        this.idProofUploadContentType = idProofUploadContentType;
    }

    public byte[] getAddressProofUploadData() {
        return addressProofUploadData;
    }

    public void setAddressProofUploadData(byte[] addressProofUploadData) {
        this.addressProofUploadData = addressProofUploadData;
    }

    public String getAddressProofUploadName() {
        return addressProofUploadName;
    }

    public void setAddressProofUploadName(String addressProofUploadName) {
        this.addressProofUploadName = addressProofUploadName;
    }

    public String getAddressProofUploadContentType() {
        return addressProofUploadContentType;
    }

    public void setAddressProofUploadContentType(String addressProofUploadContentType) {
        this.addressProofUploadContentType = addressProofUploadContentType;
    }

    public byte[] getEducationalCertificatesData() {
        return educationalCertificatesData;
    }

    public void setEducationalCertificatesData(byte[] educationalCertificatesData) {
        this.educationalCertificatesData = educationalCertificatesData;
    }

    public String getEducationalCertificatesName() {
        return educationalCertificatesName;
    }

    public void setEducationalCertificatesName(String educationalCertificatesName) {
        this.educationalCertificatesName = educationalCertificatesName;
    }

    public String getEducationalCertificatesContentType() {
        return educationalCertificatesContentType;
    }

    public void setEducationalCertificatesContentType(String educationalCertificatesContentType) {
        this.educationalCertificatesContentType = educationalCertificatesContentType;
    }

    public byte[] getExperienceCertificatesData() {
        return experienceCertificatesData;
    }

    public void setExperienceCertificatesData(byte[] experienceCertificatesData) {
        this.experienceCertificatesData = experienceCertificatesData;
    }

    public String getExperienceCertificatesName() {
        return experienceCertificatesName;
    }

    public void setExperienceCertificatesName(String experienceCertificatesName) {
        this.experienceCertificatesName = experienceCertificatesName;
    }

    public String getExperienceCertificatesContentType() {
        return experienceCertificatesContentType;
    }

    public void setExperienceCertificatesContentType(String experienceCertificatesContentType) {
        this.experienceCertificatesContentType = experienceCertificatesContentType;
    }

    public byte[] getPassportPhotoData() {
        return passportPhotoData;
    }

    public void setPassportPhotoData(byte[] passportPhotoData) {
        this.passportPhotoData = passportPhotoData;
    }

    public String getPassportPhotoName() {
        return passportPhotoName;
    }

    public void setPassportPhotoName(String passportPhotoName) {
        this.passportPhotoName = passportPhotoName;
    }

    public String getPassportPhotoContentType() {
        return passportPhotoContentType;
    }

    public void setPassportPhotoContentType(String passportPhotoContentType) {
        this.passportPhotoContentType = passportPhotoContentType;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
