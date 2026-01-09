# Kosyma-TI-Messenger-Testsuite

We maintain a private fork and a public fork of the https://github.com/gematik/TI-Messenger-Testsuite

- Parent Project from Gematik: https://github.com/gematik/TI-Messenger-Testsuite
- Our public Fork: https://github.com/kosyma-io/TI-Messenger-Testsuite
- Our private fork: https://github.com/kosyma-io/Kosyma-TI-Messenger-Testsuite

The public [TI-Messenger-Testsuite from Gematik](https://github.com/gematik/TI-Messenger-Testsuite) is the official channel for any TI-Messenger related tests. Our public [fork](https://github.com/kosyma-io/TI-Messenger-Testsuite) tracks changes in the parent and we make changes and run CI jobs within our [private fork](https://github.com/kosyma-io/Kosyma-TI-Messenger-Testsuite)

## Howto

```bash
git clone git@github.com:kosyma-io/Kosyma-TI-Messenger-Testsuite.git
cd ./Kosyma-TI-Messenger-Testsuite
git remote add public-fork git@github.com:kosyma-io/TI-Messenger-Testsuite.git

# When you have changes, just commit to the origin (kosyma-io/Kosyma-TI-Messenger-Testsuite.git)
```

When there are changes to the Gematik Project, we sync change to our public fork and rebase those changes to our private fork.

## Setup Testsuite

- Prerquisites
  - Multiple test driver instances running in parallel (see [testdriver/GETTING_STARTED.md](../testdriver/GETTING_STARTED.md) for setting that up)
  - Running Uwanja: `http//:localhost:3030`
  - Running OrgAdmin: `http//:localhost:5173`

The testsuite project requires jdk17 and maven 3.6.3.
To isolate these versions to this project, one can use sdkman.

### Install sdkman if not already installed

```bash
curl -s "https://get.sdkman.io" | bash
```
```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Confirm if it is installed correctly:

```bash
sdk version
```

### Modify .zshrc

```bash
zshconfig
```

And add the following lines to the end of the file:

```
#THIS MUST BE AT THE END OF THE FILE FOR SDKMAN TO WORK!!!
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
eval "$(vfox activate zsh)"
```

### Install the compatible JDK as well as Maven versions using sdkman env

If you have installed jdk as well as maven using other means, you should uninstall them first to avoid conflicts.
For example, run:
```bash
brew uninstall maven
brew uninstall openjdk@17
```

Install the adequate jdk and maven versions (please refer to `.sdkmanrc`):

```bash
sdk env install
```

Once installed, confirm the versions set for this project:
```bash
sdk env
```

### Install vfox

```bash
brew install vfox
```

### Run a test case defined in this testsuite

```bash
mvn clean verify \
  -Dfeature.template.dir="./src/test/resources/templates/FeatureFiles/TI-M_V2/UCs_Basis" \
  -Dcucumber.filter.tags='@TCID:TIM_V2_BASIS_AF_10X0102' \
  -Poneonly
```
