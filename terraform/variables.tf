variable "aws_region" {
  description = "Região da AWS onde os recursos serão provisionados"
  type        = string
  default     = "us-east-1"
}

variable "queue_name" {
  description = "Nome da fila SQS principal para processamento de pagamentos"
  type        = string
  default     = "payment-process-queue"
}

variable "dlq_name" {
  description = "Nome da Dead Letter Queue para falhas de processamento"
  type        = string
  default     = "payment-process-dlq"
}
