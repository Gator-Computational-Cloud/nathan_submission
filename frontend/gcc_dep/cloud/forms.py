from django import forms


class SignInForm(forms.Form):
    user = forms.CharField()
    user.label = "Username"

    pwd = forms.CharField(widget=forms.PasswordInput())
    pwd.label = "Password"


class SignUpForm(forms.Form):
    user = forms.CharField()
    user.label = "Username"

    email = forms.EmailField()
    email.label = "Email"

    pwd1 = forms.CharField(widget=forms.PasswordInput())
    pwd1.label = "Password"

    pwd2 = forms.CharField(widget=forms.PasswordInput())
    pwd2.label = "Re-enter Password"

    def clean(self):
        cleaned_data = super(SignUpForm, self).clean()

        password = cleaned_data.get("pwd1")
        password_confirm = cleaned_data.get("pwd2")

        if password and password_confirm:
            if password != password_confirm:
                raise forms.ValidationError("The two password fields must match!")
        else:
            raise forms.ValidationError("You must fill both password fields!")

        return cleaned_data


class CredentialForm(forms.Form):
    access_key = forms.CharField()
    access_key.label = "Access Key"

    secret_key = forms.CharField()
    secret_key.label = "Secret Key"

    token = forms.CharField(widget=forms.Textarea())
    token.label = "Token"


class WorkflowForm(forms.Form):
    spec = forms.FileField(required=False)
    spec.label = "Specification File"

    nodes = forms.FileField(
        required=False,
        widget=forms.ClearableFileInput(
            attrs={"multiple": True, "webkitdirectory": True, "directory": True}
        ),
    )
    nodes.label = "Node Files"
