# Generated by Django 3.1.4 on 2021-04-06 04:31

from django.conf import settings
from django.db import migrations, models
import django.db.models.deletion
import fernet_fields.fields


class Migration(migrations.Migration):

    initial = True

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name="Workflow",
            fields=[
                (
                    "id",
                    models.AutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                (
                    "name",
                    fernet_fields.fields.EncryptedCharField(
                        blank=True, default=None, max_length=255
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        default=None,
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="wf",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="Node",
            fields=[
                (
                    "id",
                    models.AutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                (
                    "name",
                    fernet_fields.fields.EncryptedCharField(
                        blank=True, default=None, max_length=255
                    ),
                ),
                (
                    "wf",
                    models.ForeignKey(
                        default=None,
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="node",
                        to="cloud.workflow",
                    ),
                ),
            ],
        ),
        migrations.CreateModel(
            name="AwsAccount",
            fields=[
                (
                    "id",
                    models.AutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                (
                    "access_key",
                    fernet_fields.fields.EncryptedCharField(
                        blank=True, default=None, max_length=255
                    ),
                ),
                (
                    "secret_key",
                    fernet_fields.fields.EncryptedCharField(
                        blank=True, default=None, max_length=255
                    ),
                ),
                (
                    "token",
                    fernet_fields.fields.EncryptedCharField(
                        blank=True, default=None, max_length=255
                    ),
                ),
                (
                    "user",
                    models.ForeignKey(
                        default=None,
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name="aws",
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
        ),
    ]
