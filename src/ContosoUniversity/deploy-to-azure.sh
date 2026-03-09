#!/bin/bash
# Azure Deployment Script for ContosoUniversity
# Execute with: ./deploy-to-azure.sh

# Default parameters - CUSTOMIZE THESE
RESOURCE_GROUP="contosouniversity-rg"
LOCATION="eastus"
APP_NAME="contosouniversity$(date +%s)"  # Unique app name
SQL_SERVER_NAME="${APP_NAME}-sqlserver"
SQL_DATABASE_NAME="ContosoUniversity"
APP_SERVICE_PLAN="${APP_NAME}-plan"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --resource-group)
      RESOURCE_GROUP="$2"
      shift 2
      ;;
    --location)
      LOCATION="$2"
      shift 2
      ;;
    --app-name)
      APP_NAME="$2"
      SQL_SERVER_NAME="${APP_NAME}-sqlserver"
      APP_SERVICE_PLAN="${APP_NAME}-plan"
      shift 2
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

echo "=========================================="
echo "Deploying ContosoUniversity to Azure"
echo "=========================================="
echo "Resource Group: $RESOURCE_GROUP"
echo "Location: $LOCATION"
echo "App Name: $APP_NAME"
echo "SQL Server: $SQL_SERVER_NAME"
echo "=========================================="

# Check prerequisites
echo "Checking prerequisites..."
if ! command -v az &> /dev/null; then
    echo "Azure CLI not found. Please install it: https://docs.microsoft.com/cli/azure/install-azure-cli"
    exit 1
fi

if ! command -v dotnet &> /dev/null; then
    echo ".NET SDK not found. Please install it."
    exit 1
fi

echo "Prerequisites satisfied."
echo

# Check if logged in to Azure
echo "Checking Azure login status..."
az account show &> /dev/null
if [ $? -ne 0 ]; then
    echo "You are not logged in to Azure. Please run 'az login' first."
    exit 1
fi

# Get current user ID for SQL admin
CURRENT_USER_ID=$(az ad signed-in-user show --query id -o tsv)
CURRENT_USER_NAME=$(az ad signed-in-user show --query userPrincipalName -o tsv)
echo "Current Azure user: $CURRENT_USER_NAME"
echo

# Create resource group
echo "Creating resource group..."
az group create --name "$RESOURCE_GROUP" --location "$LOCATION"
if [ $? -ne 0 ]; then
    echo "Failed to create resource group. Exiting."
    exit 1
fi
echo "Resource group created."
echo

# Create Azure SQL Server with Azure AD authentication
echo "Creating Azure SQL Server with Microsoft Entra ID authentication..."
az sql server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$SQL_SERVER_NAME" \
  --location "$LOCATION" \
  --enable-ad-only-auth \
  --external-admin-principal-type User \
  --external-admin-name "$CURRENT_USER_NAME" \
  --external-admin-sid "$CURRENT_USER_ID"

if [ $? -ne 0 ]; then
    echo "Failed to create SQL Server. Exiting."
    exit 1
fi
echo "SQL Server created."
echo

# Configure SQL Server firewall to allow Azure services
echo "Configuring SQL Server firewall..."
az sql server firewall-rule create \
  --resource-group "$RESOURCE_GROUP" \
  --server "$SQL_SERVER_NAME" \
  --name "AllowAzureServices" \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

echo "SQL Server firewall configured."
echo

# Create Azure SQL Database
echo "Creating Azure SQL Database..."
az sql db create \
  --resource-group "$RESOURCE_GROUP" \
  --server "$SQL_SERVER_NAME" \
  --name "$SQL_DATABASE_NAME" \
  --edition "Basic" \
  --service-objective "Basic" \
  --backup-storage-redundancy "Local"

if [ $? -ne 0 ]; then
    echo "Failed to create SQL Database. Exiting."
    exit 1
fi
echo "SQL Database created."
echo

# Create App Service Plan
echo "Creating App Service Plan..."
az appservice plan create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_SERVICE_PLAN" \
  --location "$LOCATION" \
  --sku "F1" \
  --is-linux

if [ $? -ne 0 ]; then
    echo "Failed to create App Service Plan. Exiting."
    exit 1
fi
echo "App Service Plan created."
echo

# Create Web App
echo "Creating Web App..."
az webapp create \
  --resource-group "$RESOURCE_GROUP" \
  --plan "$APP_SERVICE_PLAN" \
  --name "$APP_NAME" \
  --runtime "DOTNETCORE:9.0"

if [ $? -ne 0 ]; then
    echo "Failed to create Web App. Exiting."
    exit 1
fi
echo "Web App created."
echo

# Enable managed identity for the Web App
echo "Enabling managed identity for Web App..."
PRINCIPAL_ID=$(az webapp identity assign \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_NAME" \
  --query principalId -o tsv)

if [ -z "$PRINCIPAL_ID" ]; then
    echo "Failed to enable managed identity. Exiting."
    exit 1
fi
echo "Managed identity enabled. Principal ID: $PRINCIPAL_ID"
echo

# Get the managed identity object ID
echo "Getting managed identity details..."
MANAGED_IDENTITY_NAME=$(az webapp identity show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_NAME" \
  --query principalId -o tsv)

# Wait for Azure AD propagation
echo "Waiting for Azure AD propagation (30 seconds)..."
sleep 30

# Add managed identity as Azure AD admin on SQL Server
echo "Adding Web App managed identity as SQL administrator..."
az sql server ad-admin create \
  --resource-group "$RESOURCE_GROUP" \
  --server-name "$SQL_SERVER_NAME" \
  --display-name "$APP_NAME" \
  --object-id "$PRINCIPAL_ID"

echo "Managed identity added as SQL administrator."
echo

# Build and publish the application
echo "Building and publishing the application..."
cd "$(dirname "$0")"
dotnet publish -c Release -o ./publish

if [ $? -ne 0 ]; then
    echo "Failed to build application. Exiting."
    exit 1
fi
echo "Application built successfully."
echo

# Create deployment package
echo "Creating deployment package..."
cd publish
zip -r ../deploy.zip . > /dev/null
cd ..

if [ ! -f "deploy.zip" ]; then
    echo "Failed to create deployment package. Exiting."
    exit 1
fi
echo "Deployment package created."
echo

# Deploy to Azure App Service
echo "Deploying to Azure App Service..."
az webapp deployment source config-zip \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_NAME" \
  --src deploy.zip

if [ $? -ne 0 ]; then
    echo "Failed to deploy application. Exiting."
    exit 1
fi
echo "Application deployed successfully."
echo

# Configure connection string in App Service
echo "Configuring connection string in App Service..."
CONNECTION_STRING="Server=tcp:${SQL_SERVER_NAME}.database.windows.net;Database=${SQL_DATABASE_NAME};Authentication=Active Directory Default;TrustServerCertificate=True"

az webapp config connection-string set \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_NAME" \
  --connection-string-type "SQLAzure" \
  --settings DefaultConnection="$CONNECTION_STRING"

if [ $? -ne 0 ]; then
    echo "Failed to configure connection string. Exiting."
    exit 1
fi
echo "Connection string configured."
echo

# Clean up deployment files
echo "Cleaning up deployment files..."
rm -rf publish
rm -f deploy.zip
echo

# Get the Web App URL
WEB_APP_URL=$(az webapp show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_NAME" \
  --query defaultHostName -o tsv)

echo "=========================================="
echo "Deployment complete!"
echo "=========================================="
echo "Resource Group: $RESOURCE_GROUP"
echo "Web App URL: https://$WEB_APP_URL"
echo "SQL Server: $SQL_SERVER_NAME.database.windows.net"
echo "SQL Database: $SQL_DATABASE_NAME"
echo "=========================================="
echo
echo "Next steps:"
echo "1. Run database migrations: Navigate to the Web App in Azure Portal > Console"
echo "   and run: dotnet ef database update"
echo "2. Open the application: https://$WEB_APP_URL"
echo "=========================================="
