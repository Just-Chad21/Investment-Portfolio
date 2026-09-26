# Project Development Workflow with Claude Code

## Phase 1: Idea Validation

## Goal

Validate the idea before investing time into development.

## Tasks

* Define the problem being solved.
* Identify the target users.
* Define the value proposition.
* Analyze competitors.
* Identify technical risks.
* Determine the MVP scope.
* Decide on the tech stack and justify each choice.

## Claude Prompts

```text
Act as a startup CTO. Critique this idea and identify the biggest risks.
```

```text
What assumptions must be true for this product to succeed?
```

```text
Design a lean MVP that can be built in 30 days.
```

```text
Given these requirements and constraints, recommend a tech stack and justify each choice.
```

## Deliverables

* Problem Statement
* Value Proposition
* MVP Feature List
* Risk Assessment
* Tech Stack Decision

## Exit Criteria

- [ ] Problem and value proposition are clearly documented.
- [ ] MVP scope is agreed and bounded.
- [ ] Tech stack is decided and justified.

---

## Phase 2: Requirements Gathering

## Goal

Define exactly what the system should do.

## Tasks

* Create user personas.
* Write user stories.
* Define acceptance criteria.
* Identify functional requirements.
* Identify non-functional requirements.

## Claude Prompts

```text
Create detailed user personas for this application.
```

```text
Generate detailed user stories for this application.
```

```text
Create acceptance criteria for each user story.
```

```text
Generate functional and non-functional requirements.
```

## Deliverables

* Requirements Document
* User Stories
* Acceptance Criteria

## Exit Criteria

- [ ] All user personas created.
- [ ] User stories written and reviewed.
- [ ] Acceptance criteria defined for each story.

---

## Phase 3: System Design

## Goal

Design the architecture before coding.

## Tasks

* Define system architecture.
* Define service boundaries.
* Define external integrations.
* Create data flow diagrams.
* Identify scaling requirements.

## Claude Prompts

```text
Design a scalable architecture for this application using the agreed tech stack.
```

```text
Explain the responsibilities of each component.
```

```text
Identify potential bottlenecks.
```

## Deliverables

* Architecture Document
* Component Diagram
* Data Flow Diagram

## Exit Criteria

- [ ] Architecture documented and reviewed.
- [ ] All service boundaries and external integrations identified.

---

## Phase 4: Database Design

## Goal

Design the database structure.

## Tasks

* Define entities.
* Define relationships.
* Create ERD.
* Define indexes.
* Define constraints.

## Claude Prompts

```text
Design a normalized database schema for this application.
```

```text
Suggest indexes for optimal performance.
```

```text
Review this schema and identify weaknesses.
```

## Deliverables

* ERD
* Database Schema
* Index Strategy

## Exit Criteria

- [ ] ERD reviewed and approved.
- [ ] Schema includes indexes and constraints.

---

## Phase 5: API Design

## Goal

Design APIs before implementation.

## Tasks

* Define endpoints.
* Define request structures.
* Define response structures.
* Define validation rules.
* Define error handling.

## Claude Prompts

```text
Generate REST API specifications for this system.
```

```text
Create OpenAPI documentation.
```

```text
Define validation rules and standard error response structures for this API.
```

## Deliverables

* API Specification
* Endpoint Documentation

## Exit Criteria

- [ ] All endpoints documented with request/response structures.
- [ ] Validation rules and error handling defined.
- [ ] OpenAPI spec generated.

---

## Phase 6: Project Planning

## Goal

Break work into manageable tasks.

## Tasks

* Create epics.
* Create features.
* Create development tasks.
* Estimate effort.
* Define branching strategy and commit conventions.

## Claude Prompts

```text
Break this project into epics and implementation tasks suitable for GitHub Projects.
```

```text
Suggest a branching strategy and commit message convention for this project.
```

## Deliverables

* Product Backlog
* GitHub Board
* Sprint Plan
* Branching Strategy

## Exit Criteria

- [ ] Backlog is populated and prioritized.
- [ ] Branching strategy and commit conventions are documented.

---

## Phase 7: Development

## Goal

Build features incrementally.

## Development Cycle

For every feature:

### 1. Design

```text
Design the implementation for this feature.
```

### 2. Generate

```text
Generate the implementation for this feature.
```

### 3. Test

```text
Generate unit tests for this implementation.
```

### 4. Review

```text
Review this implementation and identify problems.
```

```text
Review this implementation for OWASP Top 10 security issues.
```

### 5. Refactor

```text
Suggest improvements using clean architecture principles.
```

## Deliverables

* Production Code
* Unit Tests
* Code Reviews

## Exit Criteria

- [ ] All acceptance criteria pass.
- [ ] Unit tests written and passing.
- [ ] Code reviewed and refactored.

---

## Phase 8: Testing

## Goal

Validate the full system with integration and edge case testing. Unit tests are written per-feature in Phase 7; this phase covers cross-cutting concerns.

## Tasks

* Integration tests
* Edge case testing
* Performance testing

## Claude Prompts

```text
Generate integration tests for this feature.
```

```text
What edge cases am I missing?
```

## Deliverables

* Integration Test Suite
* Test Report

## Exit Criteria

- [ ] Integration tests written and passing.
- [ ] Edge cases identified and tested.
- [ ] Performance benchmarks meet non-functional requirements.

---

## Phase 9: DevOps

## Goal

Automate build, test, and deployment.

## Tasks

* Docker setup
* CI/CD setup
* Environment management
* Monitoring setup

## Claude Prompts

```text
Create a Dockerfile and docker-compose setup.
```

```text
Generate a GitHub Actions workflow.
```

```text
Configure Prometheus and Grafana for this project.
```

## Deliverables

* Docker Configuration
* CI/CD Pipeline
* Monitoring Stack

## Exit Criteria

- [ ] CI pipeline runs on every PR.
- [ ] Docker images build and run correctly.
- [ ] Monitoring and alerting configured.

---

## Phase 10: Security Review

## Goal

Identify and remediate security weaknesses before launch. Lightweight security checks happen per-feature in Phase 7; this phase is a full audit.

## Tasks

* Authentication review
* Authorization review
* Input validation review
* Secret management review
* Dependency scanning

## Claude Prompts

```text
Perform a security audit on this codebase.
```

```text
Identify OWASP Top 10 vulnerabilities.
```

## Deliverables

* Security Audit Report
* Security Fixes

## Exit Criteria

- [ ] All critical and high findings resolved.
- [ ] Dependency scan clean.
- [ ] Secrets not stored in code or version control.

---

## Phase 11: Documentation

## Goal

Create maintainable documentation.

## Tasks

* README
* API Docs
* Architecture Docs
* Deployment Guide

## Claude Prompts

```text
Generate production-ready documentation for this repository.
```

## Deliverables

* README
* API Documentation
* Architecture Documentation
* Deployment Guide

## Exit Criteria

- [ ] README covers setup, usage, and contribution.
- [ ] API docs are accurate against the implemented endpoints.
- [ ] Deployment guide tested against a clean environment.

---

## Phase 12: Release

## Goal

Deploy and monitor production.

## Tasks

* Production deployment
* Health checks
* Monitoring
* Logging
* Alerting

## Claude Prompts

```text
Create a production deployment checklist.
```

```text
Review my deployment configuration.
```

## Deliverables

* Production Environment
* Monitoring Dashboards
* Alerting Rules

## Exit Criteria

- [ ] Deployment checklist completed.
- [ ] Health checks passing.
- [ ] Monitoring dashboards live and alerting configured.

---

## Per-Feature Inner Loop

Use this checklist for every feature built during Phase 7 and beyond:

1. Define Requirements
2. Design Solution
3. Generate Implementation
4. Write Tests
5. Review Code
6. Security Check
7. Refactor
8. Document
9. Commit
10. Deploy
11. Monitor

### Golden Rule

Never ask:

```text
Build my application.
```

Instead ask:

```text
Design this feature.
```

```text
Implement this feature.
```

```text
Test this feature.
```

```text
Review this feature.
```

```text
Refactor this feature.
```

Treat Claude Code as:

* Software Architect
* Senior Engineer
* Code Reviewer
* QA Engineer
* Technical Writer
* DevOps Assistant

Not just a code generator.
