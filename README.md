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