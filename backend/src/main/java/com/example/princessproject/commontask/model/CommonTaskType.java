package com.example.princessproject.commontask.model;

/**
 * The 3 "공통 과제" every participant does regardless of which 아비투스/자본 they picked
 * (see the notice rendered above the goal picker in SelectionWizard). Unlike UserMission,
 * these aren't part of the weighted goal/stat tree - they're tracked and shown separately so
 * picking capitals never accidentally hides or excludes them.
 */
public enum CommonTaskType {
    READING,
    STUDY
}
