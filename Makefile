# Makefile for setting up the project
OS := $(shell uname -s)

# init 타깃: 프로젝트 초기 설정
init:
ifeq ($(OS), Linux)
	@echo "Detected Linux. Setting core.autocrlf to input..."
	git config core.autocrlf input
else ifeq ($(OS), Darwin)
	@echo "Detected macOS. Setting core.autocrlf to input..."
	git config core.autocrlf input
else ifeq ($(OS), Windows_NT)
	@echo "Detected Windows. Setting core.autocrlf to true..."
	git config core.autocrlf true
else
	@echo "Unknown OS. No changes made to core.autocrlf."
endif
	@echo "Setting commit template..."
	git config commit.template .gitmessage
	# Git 사용자 이름과 이메일 설정
	# git config user.name {name}
	@echo `git config user.name`
	# git config user.email {email}
	@echo `git config user.email`
